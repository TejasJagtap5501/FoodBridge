package com.foodbridge.donation;

import com.foodbridge.common.PageRequestFactory;
import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.dto.*;
import com.foodbridge.exception.ApiException;
import com.foodbridge.impact.ImpactLog;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.NgoProfile;
import com.foodbridge.user.NgoProfileRepository;
import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserRepository;
import com.foodbridge.user.UserStatus;
import com.foodbridge.user.VerificationStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Donation lifecycle: creation, editing, cancellation, search and completion.
 *
 * <p>Every state change funnels through this service and is guarded by
 * {@link DonationStatus#canTransitionTo}, so an invalid transition cannot be
 * reached even if the frontend is bypassed.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> SORTABLE = Set.of("createdAt", "expiresAt", "quantity", "foodName", "status");

    /** Rough meal estimate used for impact reporting; one meal ~ 0.4 kg of food. */
    static final BigDecimal KG_PER_MEAL = new BigDecimal("0.4");

    private final DonationRepository donationRepository;
    private final FoodCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ImpactLogRepository impactLogRepository;
    private final NgoProfileRepository ngoProfileRepository;

    // ------------------------------------------------------------------
    // Creation and editing
    // ------------------------------------------------------------------

    @Transactional
    public DonationResponse create(AuthenticatedUser actor, DonationCreateRequest request) {
        if (actor.role() != Role.DONOR) {
            throw ApiException.forbidden("ROLE_REQUIRED", "Only donor accounts can publish donations");
        }
        validateExpiryWindow(request.preparedAt(), request.expiresAt());

        FoodCategory category = categoryRepository.findById(request.categoryId())
                .filter(FoodCategory::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Selected food category is not available"));
        User donor = userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required"));

        Donation donation = new Donation();
        donation.setReferenceCode(nextReferenceCode());
        donation.setDonor(donor);
        donation.setCategory(category);
        applyFields(donation, request.foodName(), request.description(), request.quantity(),
                request.quantityUnit(), request.foodType(), request.preparedAt(), request.expiresAt(),
                request.pickupAddress(), request.city(), request.pickupInstructions(),
                request.storageConditions(), request.allergenInfo(), request.handlingInstructions());
        donation.setSafetyDeclaration(request.safetyDeclaration());
        donation.setEstimatedMeals(estimateMeals(request.quantity(), request.quantityUnit()));
        donation.setStatus(DonationStatus.AVAILABLE);

        Donation saved = donationRepository.save(donation);
        log.info("Donation {} created by donor {} ({} {})", saved.getReferenceCode(), actor.id(),
                saved.getQuantity(), saved.getQuantityUnit());

        // --- live fan-out to donors' community: every verified NGO and every
        // administrator is told that fresh food is available. Notifications are
        // part of this transaction, so a rolled-back donation never notifies.
        notifyNewDonation(saved, donor);
        return DonationResponse.from(saved, true);
    }

    /**
     * Fan out a DONATION_CREATED notification to verified NGOs (the only accounts
     * that can claim) and to administrators (who monitor platform activity).
     *
     * <p>Recipients are resolved here rather than in
     * {@link com.foodbridge.notification.NotificationService} so the notification
     * module stays unaware of donation audience rules.</p>
     */
    private void notifyNewDonation(Donation donation, User donor) {
        String title = "New donation available";
        String message = donor.getFullName() + " (" + donation.getCity() + ") published \""
                + donation.getFoodName() + "\" — " + donation.getQuantity().stripTrailingZeros().toPlainString()
                + " " + donation.getQuantityUnit() + ", expires in " + describeWindow(donation.getExpiresAt()) + ".";
        String link = "/ngo/donations/" + donation.getId();

        for (NgoProfile profile : ngoProfileRepository.findAllByVerificationStatus(VerificationStatus.APPROVED)) {
            notificationService.create(profile.getUser().getId(), NotificationType.DONATION_CREATED, title, message, link);
        }
        for (User admin : userRepository.findAllByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)) {
            notificationService.create(admin.getId(), NotificationType.DONATION_CREATED, title, message,
                    "/admin/donations");
        }
    }

    /** Human-friendly remaining shelf life, e.g. "6h 0m". */
    private static String describeWindow(Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        long hours = Math.max(0, remaining.toHours());
        long minutes = Math.max(0, remaining.toMinutesPart());
        return hours + "h " + minutes + "m";
    }

    @Transactional
    public DonationResponse update(AuthenticatedUser actor, Long donationId, DonationUpdateRequest request) {
        Donation donation = loadOwned(actor, donationId);
        requireStatus(donation, DonationStatus.AVAILABLE, "Only available donations can be edited");
        validateExpiryWindow(request.preparedAt(), request.expiresAt());

        FoodCategory category = categoryRepository.findById(request.categoryId())
                .filter(FoodCategory::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Selected food category is not available"));

        donation.setCategory(category);
        applyFields(donation, request.foodName(), request.description(), request.quantity(),
                request.quantityUnit(), request.foodType(), request.preparedAt(), request.expiresAt(),
                request.pickupAddress(), request.city(), request.pickupInstructions(),
                request.storageConditions(), request.allergenInfo(), request.handlingInstructions());
        donation.setEstimatedMeals(estimateMeals(request.quantity(), request.quantityUnit()));
        Donation saved = donationRepository.save(donation);
        log.info("Donation {} updated by donor {}", saved.getReferenceCode(), actor.id());
        return DonationResponse.from(saved, true);
    }

    @Transactional
    public DonationResponse cancelByDonor(AuthenticatedUser actor, Long donationId, String reason) {
        Donation donation = loadOwned(actor, donationId);
        if (donation.getStatus() == DonationStatus.COMPLETED) {
            throw ApiException.conflict("INVALID_TRANSITION", "Completed donations cannot be cancelled");
        }
        if (donation.getStatus() == DonationStatus.CLAIMED) {
            throw ApiException.conflict("CLAIM_ACTIVE",
                    "This donation is already claimed. Cancel the claim or contact the NGO instead.");
        }
        if (!donation.getStatus().canTransitionTo(DonationStatus.CANCELLED)) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "A " + donation.getStatus() + " donation cannot be cancelled");
        }
        donation.setStatus(DonationStatus.CANCELLED);
        Donation saved = donationRepository.save(donation);
        log.info("Donation {} cancelled by donor {}: {}", saved.getReferenceCode(), actor.id(), reason);
        return DonationResponse.from(saved, true);
    }

    /**
     * Completion is triggered by the pickup workflow. Called inside the
     * surrounding transaction; writes the immutable impact record exactly once.
     */
    @Transactional
    public Donation completeDonation(Donation donation, Long ngoId) {
        if (!donation.getStatus().canTransitionTo(DonationStatus.COMPLETED)) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "Only claimed donations can be completed (current: " + donation.getStatus() + ")");
        }
        Instant now = Instant.now();
        int updated = donationRepository.transition(donation.getId(), DonationStatus.CLAIMED, DonationStatus.COMPLETED);
        if (updated == 0) {
            throw ApiException.conflict("INVALID_TRANSITION", "Donation state changed concurrently, please reload");
        }

        if (impactLogRepository.findByDonationId(donation.getId()).isEmpty()) {
            ImpactLog impact = new ImpactLog();
            impact.setDonationId(donation.getId());
            impact.setDonorId(donation.getDonor().getId());
            impact.setNgoId(ngoId);
            impact.setQuantity(donation.getQuantity());
            impact.setQuantityUnit(donation.getQuantityUnit());
            impact.setMealsServed(donation.getEstimatedMeals());
            impact.setCity(donation.getCity());
            impact.setCategoryId(donation.getCategory().getId());
            impact.setCompletedAt(now);
            impactLogRepository.save(impact);
        }

        notificationService.create(donation.getDonor().getId(), NotificationType.DONATION_COMPLETED,
                "Donation completed",
                donation.getFoodName() + " was collected. Thank you for rescuing " + donation.getEstimatedMeals()
                        + " meals worth of food!",
                "/donor/donations/" + donation.getId());
        return donation;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DonationResponse getDonation(AuthenticatedUser actor, Long donationId) {
        Donation donation = donationRepository.findDetailedById(donationId)
                .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
        boolean owner = donation.getDonor().getId().equals(actor.id());
        boolean admin = actor.role() == Role.ADMIN;
        boolean ngo = actor.role() == Role.NGO;
        if (!owner && !admin && !ngo) {
            // Defence in depth - URL rules already restrict these endpoints.
            throw ApiException.forbidden("ACCESS_DENIED", "You cannot view this donation");
        }
        return DonationResponse.from(donation, owner || admin);
    }

    /** Donor's own list with server-side search, filtering, sorting and pagination. */
    @Transactional(readOnly = true)
    public PageResponse<DonationSummaryResponse> listForDonor(AuthenticatedUser actor, DonationStatus status,
                                                             Long categoryId, String search, String city,
                                                             Instant expiringBefore, int page, int size,
                                                             String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, SORTABLE);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Specification<Donation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("donor").get("id"), actor.id()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("foodName")), like),
                        cb.like(cb.lower(root.get("referenceCode")), like)));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT)));
            }
            if (expiringBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expiresAt"), expiringBefore));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Donation> result = donationRepository.findAll(spec, pageable);
        return PageResponse.of(result, DonationSummaryResponse::from);
    }

    /** NGO-facing browse: only AVAILABLE, non-expired donations are ever returned. */
    @Transactional(readOnly = true)
    public PageResponse<DonationSummaryResponse> browseAvailable(Long categoryId, String search, String city,
                                                                 BigDecimal minQuantity, Instant expiringBefore,
                                                                 int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, SORTABLE);
        Pageable effective = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "expiresAt"))
                : pageable;

        Specification<Donation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), DonationStatus.AVAILABLE));
            predicates.add(cb.greaterThan(root.get("expiresAt"), Instant.now()));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("foodName")), like));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT)));
            }
            if (minQuantity != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity));
            }
            if (expiringBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expiresAt"), expiringBefore));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.of(donationRepository.findAll(spec, effective), DonationSummaryResponse::from);
    }

    /** Admin list across all donors and statuses. */
    @Transactional(readOnly = true)
    public PageResponse<DonationSummaryResponse> listForAdmin(DonationStatus status, Long donorId, Long categoryId,
                                                              String search, String city, int page, int size,
                                                              String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, SORTABLE);
        Pageable effective = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;
        Specification<Donation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (donorId != null) {
                predicates.add(cb.equal(root.get("donor").get("id"), donorId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("foodName")), like),
                        cb.like(cb.lower(root.get("referenceCode")), like)));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.of(donationRepository.findAll(spec, effective), DonationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public DonationStatsResponse statsForDonor(Long donorId) {
        long total = donationRepository.countByDonorId(donorId);
        long available = donationRepository.countByDonorIdAndStatus(donorId, DonationStatus.AVAILABLE);
        long claimed = donationRepository.countByDonorIdAndStatus(donorId, DonationStatus.CLAIMED);
        long completed = donationRepository.countByDonorIdAndStatus(donorId, DonationStatus.COMPLETED);
        long expired = donationRepository.countByDonorIdAndStatus(donorId, DonationStatus.EXPIRED);
        long cancelled = donationRepository.countByDonorIdAndStatus(donorId, DonationStatus.CANCELLED);
        long meals = impactLogRepository.sumMealsByDonor(donorId);
        return new DonationStatsResponse(total, available, claimed, completed, expired, cancelled,
                meals, meals, 0);
    }

    @Transactional(readOnly = true)
    public Donation loadForRead(Long donationId) {
        return donationRepository.findDetailedById(donationId)
                .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Donation loadOwned(AuthenticatedUser actor, Long donationId) {
        Donation donation = donationRepository.findDetailedById(donationId)
                .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
        if (!donation.getDonor().getId().equals(actor.id())) {
            // Ownership check happens in the service, not only in the controller.
            throw ApiException.forbidden("ACCESS_DENIED", "This donation belongs to another donor");
        }
        return donation;
    }

    private void requireStatus(Donation donation, DonationStatus expected, String message) {
        if (donation.getStatus() != expected) {
            throw ApiException.conflict("INVALID_TRANSITION", message);
        }
    }

    private void validateExpiryWindow(Instant preparedAt, Instant expiresAt) {
        if (expiresAt.isBefore(preparedAt)) {
            throw ApiException.badRequest("INVALID_EXPIRY", "Expiry time must be after the preparation time");
        }
        Duration window = Duration.between(preparedAt, expiresAt);
        if (window.toHours() > 48) {
            throw ApiException.badRequest("INVALID_EXPIRY",
                    "A donation may not stay listed for more than 48 hours after preparation");
        }
    }

    private void applyFields(Donation donation, String foodName, String description, BigDecimal quantity,
                             String quantityUnit, FoodType foodType, Instant preparedAt, Instant expiresAt,
                             String pickupAddress, String city, String pickupInstructions,
                             String storageConditions, String allergenInfo, String handlingInstructions) {
        donation.setFoodName(foodName.trim());
        donation.setDescription(trimToNull(description));
        donation.setQuantity(quantity);
        donation.setQuantityUnit(quantityUnit.trim());
        donation.setFoodType(foodType);
        donation.setPreparedAt(preparedAt);
        donation.setExpiresAt(expiresAt);
        donation.setPickupAddress(pickupAddress.trim());
        donation.setCity(city.trim());
        donation.setPickupInstructions(trimToNull(pickupInstructions));
        donation.setStorageConditions(trimToNull(storageConditions));
        donation.setAllergenInfo(trimToNull(allergenInfo));
        donation.setHandlingInstructions(trimToNull(handlingInstructions));
    }

    /**
     * Converts a donation quantity into an approximate meal count.
     *
     * <ul>
     *   <li>Weight units are converted to kilograms and divided by the
     *       {@value #KG_PER_MEAL} kg-per-meal constant.</li>
     *   <li>Countable units (servings, meals, packets, boxes, portions) already
     *       represent portions, so they are used directly.</li>
     *   <li>Unknown units are assumed to be kilograms.</li>
     * </ul>
     * The result is always at least 1 for any positive quantity.
     */
    static int estimateMeals(BigDecimal quantity, String unit) {
        if (quantity == null) {
            return 0;
        }
        String normalized = unit == null ? "" : unit.trim().toUpperCase(Locale.ROOT);
        BigDecimal meals = switch (normalized) {
            // Weight units are converted to kilograms, then to servings.
            case "KG", "KGS", "KILOGRAM", "KILOGRAMS" -> quantity.divide(KG_PER_MEAL, 0, java.math.RoundingMode.HALF_UP);
            case "G", "GRAM", "GRAMS" -> quantity.divide(BigDecimal.valueOf(400), 0, java.math.RoundingMode.HALF_UP);
            // Countable units already represent portions.
            case "SERVINGS", "MEALS", "PACKETS", "BOXES", "PORTIONS" -> quantity;
            // Unknown unit: assume the quantity is expressed in kilograms.
            default -> quantity.divide(KG_PER_MEAL, 0, java.math.RoundingMode.HALF_UP);
        };
        return Math.max(1, meals.intValue());
    }

    private static String nextReferenceCode() {
        StringBuilder sb = new StringBuilder("FB-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
