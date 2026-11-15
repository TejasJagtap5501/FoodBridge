package com.foodbridge.config;

import com.foodbridge.claim.Claim;
import com.foodbridge.claim.ClaimRepository;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationRepository;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.donation.FoodCategory;
import com.foodbridge.donation.FoodCategoryRepository;
import com.foodbridge.donation.FoodType;
import com.foodbridge.impact.ImpactLog;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.notification.Notification;
import com.foodbridge.notification.NotificationRepository;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.pickup.Pickup;
import com.foodbridge.pickup.PickupRepository;
import com.foodbridge.pickup.PickupStatus;
import com.foodbridge.request.NgoRequest;
import com.foodbridge.request.NgoRequestRepository;
import com.foodbridge.request.NgoRequestStatus;
import com.foodbridge.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds a realistic development dataset so the dashboards, charts, filters and
 * claim workflow can be demonstrated immediately.
 *
 * <p>Disabled with {@code SEED_DEMO_DATA=false}. It never runs when data already
 * exists, so restarts are idempotent. Demo credentials are documented in the
 * README; the passwords are still BCrypt hashed in the database.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final FoodBridgeProperties properties;
    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final FoodCategoryRepository categoryRepository;
    private final DonationRepository donationRepository;
    private final ClaimRepository claimRepository;
    private final PickupRepository pickupRepository;
    private final NotificationRepository notificationRepository;
    private final NgoRequestRepository ngoRequestRepository;
    private final ImpactLogRepository impactLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getSeed().isDemoData()) {
            log.info("Demo data seeding disabled (SEED_DEMO_DATA=false)");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Existing users found - skipping demo data seeding");
            return;
        }
        log.info("Seeding demo data...");

        User admin = user("admin@foodbridge.dev", "Admin@12345", "Priya Deshmukh", Role.ADMIN, UserStatus.ACTIVE, "+91 98200 11111");

        User spiceRoute = donor("donor@foodbridge.dev", "Donor@12345", "Rohan Kulkarni",
                "Spice Route Kitchen", "RESTAURANT", "12 FC Road", "Pune", "Maharashtra", "411004", "+91 98200 22222");
        User sunriseHotels = donor("hotel@foodbridge.dev", "Hotel@12345", "Meera Iyer",
                "Sunrise Grand Hotel", "HOTEL", "45 Baner Road", "Pune", "Maharashtra", "411045", "+91 98200 33333");
        User dailyBakery = donor("bakery@foodbridge.dev", "Bakery@12345", "Aarav Shah",
                "Daily Crumb Bakery", "BAKERY", "8 Koregaon Park", "Pune", "Maharashtra", "411001", "+91 98200 44444");

        User ngoApproved = ngo("ngo@foodbridge.dev", "Ngo@12345", "Ananya Rao",
                "Annapurna Seva Trust", "MH/2019/0123456", "Pune", VerificationStatus.APPROVED, UserStatus.ACTIVE);
        User ngoSecond = ngo("ngo2@foodbridge.dev", "Ngo2@12345", "Sameer Joshi",
                "Roti Bank Collective", "MH/2021/0987654", "Pune", VerificationStatus.APPROVED, UserStatus.ACTIVE);
        User ngoPending = ngo("ngo.pending@foodbridge.dev", "Ngo@12345", "Kavita Nair",
                "Asha Foundation", "MH/2024/0554433", "Pune", VerificationStatus.PENDING, UserStatus.PENDING);

        FoodCategory cooked = category("Cooked Meals");
        FoodCategory bakery = category("Bakery & Bread");
        FoodCategory produce = category("Fresh Produce");
        FoodCategory packaged = category("Packaged & Dry Goods");

        // --- completed history with impact logs -----------------------
        for (int i = 0; i < 8; i++) {
            Donation d = donation(spiceRoute, pick(cooked, bakery, packaged),
                    pick("Vegetable Biryani", "Dal Khichdi", "Paneer Curry & Rice", "Veg Pulao"),
                    new BigDecimal(pick("6", "8", "10", "12")), "KG", FoodType.VEGETARIAN,
                    "Pune", "Spice Route Kitchen, 12 FC Road, Pune", i + 3);
            completedHistory(d, ngoApproved, i + 2);
        }
        for (int i = 0; i < 5; i++) {
            Donation d = donation(sunriseHotels, pick(cooked, bakery),
                    pick("Assorted Pastries", "Breakfast Buffet Surplus", "Sandwich Platters"),
                    new BigDecimal(pick("4", "5", "7")), "KG", FoodType.MIXED,
                    "Pune", "Sunrise Grand Hotel, 45 Baner Road, Pune", i + 5);
            completedHistory(d, ngoSecond, i + 1);
        }
        for (int i = 0; i < 3; i++) {
            Donation d = donation(dailyBakery, bakery,
                    pick("Fresh Bread Loaves", "Croissants", "Whole Wheat Buns"),
                    new BigDecimal(pick("2", "3", "4")), "KG", FoodType.VEGETARIAN,
                    "Pune", "Daily Crumb Bakery, 8 Koregaon Park, Pune", i + 2);
            completedHistory(d, ngoApproved, i + 1);
        }

        // --- available donations --------------------------------------
        donation(spiceRoute, cooked, "Veg Biryani (Dinner Surplus)", new BigDecimal("9"), "KG",
                FoodType.VEGETARIAN, "Pune", "Spice Route Kitchen, 12 FC Road, Pune", 5);
        donation(spiceRoute, cooked, "Rajma Chawal", new BigDecimal("6.5"), "KG",
                FoodType.VEGETARIAN, "Pune", "Spice Route Kitchen, 12 FC Road, Pune", 3);
        donation(sunriseHotels, cooked, "Continental Buffet Surplus", new BigDecimal("12"), "KG",
                FoodType.MIXED, "Pune", "Sunrise Grand Hotel, 45 Baner Road, Pune", 6);
        donation(sunriseHotels, produce, "Fresh Fruit Platters", new BigDecimal("5"), "KG",
                FoodType.VEGAN, "Pune", "Sunrise Grand Hotel, 45 Baner Road, Pune", 4);
        donation(dailyBakery, bakery, "Sourdough & Baguettes", new BigDecimal("4"), "KG",
                FoodType.VEGETARIAN, "Pune", "Daily Crumb Bakery, 8 Koregaon Park, Pune", 8);
        donation(dailyBakery, packaged, "Sealed Biscuit Cartons", new BigDecimal("30"), "PACKETS",
                FoodType.VEGETARIAN, "Pune", "Daily Crumb Bakery, 8 Koregaon Park, Pune", 24 * 3);

        // --- claimed donations (pending pickups) ----------------------
        Donation claimedOne = donation(sunriseHotels, cooked, "Chicken Curry & Rice", new BigDecimal("8"), "KG",
                FoodType.NON_VEGETARIAN, "Pune", "Sunrise Grand Hotel, 45 Baner Road, Pune", 4);
        Claim claimOne = activeClaim(claimedOne, ngoApproved, "Can collect within the hour");
        schedule(claimOne, ngoApproved, 1);

        Donation claimedTwo = donation(spiceRoute, cooked, "Paneer Butter Masala", new BigDecimal("5"), "KG",
                FoodType.VEGETARIAN, "Pune", "Spice Route Kitchen, 12 FC Road, Pune", 5);
        activeClaim(claimedTwo, ngoSecond, "Picking up at 8 PM");

        // --- expired donation -----------------------------------------
        Donation expired = donation(spiceRoute, cooked, "Leftover Pasta", new BigDecimal("3"), "KG",
                FoodType.VEGETARIAN, "Pune", "Spice Route Kitchen, 12 FC Road, Pune", -2);

        // --- cancelled donation ---------------------------------------
        Donation cancelled = donation(dailyBakery, bakery, "Cinnamon Rolls", new BigDecimal("2"), "KG",
                FoodType.VEGETARIAN, "Pune", "Daily Crumb Bakery, 8 Koregaon Park, Pune", 6);
        cancelled.setStatus(DonationStatus.CANCELLED);

        // --- NGO requirement requests ---------------------------------
        NgoRequest request = new NgoRequest();
        request.setNgo(ngoApproved);
        request.setTitle("Hot meals for 120 shelter residents");
        request.setDescription("We serve dinner at the Shivajinagar shelter every night and need cooked meals.");
        request.setCategory(cooked);
        request.setQuantityNeeded(new BigDecimal("20"));
        request.setQuantityUnit("KG");
        request.setCity("Pune");
        request.setNeededBy(Instant.now().plusSeconds(2 * 86_400));
        request.setBeneficiaries(120);
        request.setStatus(NgoRequestStatus.OPEN);
        ngoRequestRepository.save(request);

        NgoRequest request2 = new NgoRequest();
        request2.setNgo(ngoSecond);
        request2.setTitle("Bread and bakery items for morning distribution");
        request2.setDescription("Weekly bread distribution to construction workers.");
        request2.setCategory(bakery);
        request2.setQuantityNeeded(new BigDecimal("10"));
        request2.setQuantityUnit("KG");
        request2.setCity("Pune");
        request2.setNeededBy(Instant.now().plusSeconds(3 * 86_400));
        request2.setBeneficiaries(60);
        request2.setStatus(NgoRequestStatus.OPEN);
        ngoRequestRepository.save(request2);

        notificationRepository.save(notification(ngoPending.getId(), NotificationType.NGO_VERIFICATION,
                "Application under review",
                "Your organisation verification is being reviewed by the FoodBridge team."));

        log.info("Demo data ready: {} users, {} donations, {} claims",
                userRepository.count(), donationRepository.count(), claimRepository.count());
        log.info("Demo logins: admin@foodbridge.dev / Admin@12345, donor@foodbridge.dev / Donor@12345, "
                + "ngo@foodbridge.dev / Ngo@12345 (pending NGO: ngo.pending@foodbridge.dev / Ngo@12345)");
    }

    // ------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------

    private User user(String email, String password, String name, Role role, UserStatus status, String phone) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setStatus(status);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private User donor(String email, String password, String contact, String business, String type,
                       String address, String city, String state, String postal, String phone) {
        User u = user(email, password, contact, Role.DONOR, UserStatus.ACTIVE, phone);
        DonorProfile p = new DonorProfile();
        p.setUser(u);
        p.setBusinessName(business);
        p.setBusinessType(type);
        p.setContactPerson(contact);
        p.setAddressLine1(address);
        p.setCity(city);
        p.setState(state);
        p.setPostalCode(postal);
        p.setFssaiLicense("1152100" + ThreadLocalRandom.current().nextInt(100000, 999999));
        donorProfileRepository.save(p);
        return u;
    }

    private User ngo(String email, String password, String contact, String org, String reg, String city,
                     VerificationStatus verification, UserStatus status) {
        User u = user(email, password, contact, Role.NGO, status, "+91 98200 55555");
        NgoProfile p = new NgoProfile();
        p.setUser(u);
        p.setOrganizationName(org);
        p.setRegistrationNumber(reg);
        p.setContactPerson(contact);
        p.setWebsite("https://" + org.toLowerCase().replaceAll("[^a-z]", "") + ".example.org");
        p.setAddressLine1("Community Centre, " + city);
        p.setCity(city);
        p.setState("Maharashtra");
        p.setPostalCode("411005");
        p.setVerificationStatus(verification);
        if (verification != VerificationStatus.PENDING) {
            p.setVerifiedAt(Instant.now().minusSeconds(86_400L * 5));
        }
        ngoProfileRepository.save(p);
        return u;
    }

    private FoodCategory category(String name) {
        return categoryRepository.findBySlug(slug(name))
                .orElseGet(() -> categoryRepository.findAll().stream()
                        .filter(c -> c.getName().equals(name)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Category " + name + " missing")));
    }

    private static String slug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private Donation donation(User donor, FoodCategory category, String foodName, BigDecimal qty, String unit,
                              FoodType type, String city, String address, int hoursUntilExpiry) {
        Donation d = new Donation();
        d.setReferenceCode("FB-DEMO" + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999)));
        d.setDonor(donor);
        d.setCategory(category);
        d.setFoodName(foodName);
        d.setDescription("Freshly prepared surplus food from today's service. Stored under recommended conditions.");
        d.setQuantity(qty);
        d.setQuantityUnit(unit);
        d.setFoodType(type);
        d.setPreparedAt(Instant.now().minusSeconds(3600));
        d.setExpiresAt(Instant.now().plusSeconds(hoursUntilExpiry * 3600L));
        d.setPickupAddress(address);
        d.setCity(city);
        d.setPickupInstructions("Ask for the kitchen manager at the service entrance.");
        d.setStorageConditions(category.getName().contains("Cooked")
                ? "Keep refrigerated below 5°C until pickup"
                : "Store in a cool, dry place");
        d.setAllergenInfo(category.getName().contains("Bakery")
                ? "Contains gluten, may contain traces of nuts"
                : "May contain dairy and nuts");
        d.setHandlingInstructions("Use clean, food-grade containers for transport.");
        d.setSafetyDeclaration(true);
        d.setEstimatedMeals(Math.max(1, qty.multiply(new BigDecimal("2.5")).intValue()));
        d.setStatus(hoursUntilExpiry < 0 ? DonationStatus.EXPIRED : DonationStatus.AVAILABLE);
        return donationRepository.save(d);
    }

    private void completedHistory(Donation donation, User ngo, int daysAgo) {
        Instant completedAt = Instant.now().minusSeconds(86_400L * daysAgo);
        donation.setStatus(DonationStatus.COMPLETED);
        donationRepository.save(donation);

        Claim claim = new Claim();
        claim.setDonation(donation);
        claim.setNgo(ngo);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setSuitabilityCheck(true);
        claim.setClaimedAt(completedAt.minusSeconds(7200));
        claim.setResolvedAt(completedAt);
        claimRepository.save(claim);

        Pickup pickup = new Pickup();
        pickup.setClaim(claim);
        pickup.setScheduledAt(completedAt.minusSeconds(1800));
        pickup.setStatus(PickupStatus.COMPLETED);
        pickup.setDonorConfirmedAt(completedAt.minusSeconds(600));
        pickup.setNgoConfirmedAt(completedAt);
        pickup.setCompletedAt(completedAt);
        pickup.setCreatedBy(ngo.getId());
        pickupRepository.save(pickup);

        ImpactLog impact = new ImpactLog();
        impact.setDonationId(donation.getId());
        impact.setDonorId(donation.getDonor().getId());
        impact.setNgoId(ngo.getId());
        impact.setQuantity(donation.getQuantity());
        impact.setQuantityUnit(donation.getQuantityUnit());
        impact.setMealsServed(donation.getEstimatedMeals());
        impact.setCity(donation.getCity());
        impact.setCategoryId(donation.getCategory().getId());
        impact.setCompletedAt(completedAt);
        impactLogRepository.save(impact);
    }

    private Claim activeClaim(Donation donation, User ngo, String message) {
        donation.setStatus(DonationStatus.CLAIMED);
        donationRepository.save(donation);
        Claim claim = new Claim();
        claim.setDonation(donation);
        claim.setNgo(ngo);
        claim.setStatus(ClaimStatus.ACTIVE);
        claim.setMessage(message);
        claim.setSuitabilityCheck(true);
        claim.setClaimedAt(Instant.now().minusSeconds(1800));
        return claimRepository.save(claim);
    }

    private void schedule(Claim claim, User ngo, int hoursFromNow) {
        Pickup pickup = new Pickup();
        pickup.setClaim(claim);
        pickup.setScheduledAt(Instant.now().plusSeconds(hoursFromNow * 3600L));
        pickup.setStatus(PickupStatus.SCHEDULED);
        pickup.setInstructions("Collect from the loading bay; ring the bell twice.");
        pickup.setCreatedBy(ngo.getId());
        pickupRepository.save(pickup);
    }

    private Notification notification(Long recipient, NotificationType type, String title, String message) {
        Notification n = new Notification();
        n.setRecipientId(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        return n;
    }

    private static <T> T pick(T... values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
