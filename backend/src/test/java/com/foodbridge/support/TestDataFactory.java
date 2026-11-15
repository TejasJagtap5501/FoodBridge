package com.foodbridge.support;

import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.donation.FoodCategory;
import com.foodbridge.donation.FoodCategoryRepository;
import com.foodbridge.donation.FoodType;
import com.foodbridge.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds persisted fixtures for integration tests. Kept deliberately explicit so
 * a test reads like the business scenario it represents.
 */
@Component
public class TestDataFactory {

    public static final String PASSWORD = "TestPass123";

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final FoodCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AtomicInteger counter = new AtomicInteger();

    public TestDataFactory(UserRepository userRepository,
                           DonorProfileRepository donorProfileRepository,
                           NgoProfileRepository ngoProfileRepository,
                           FoodCategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void reset() {
        counter.set(0);
    }

    public User donor() {
        return donor("donor");
    }

    public User donor(String prefix) {
        String suffix = prefix + counter.incrementAndGet();
        User user = new User();
        user.setEmail(suffix + "@example.org");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Donor " + suffix);
        user.setPhone("+91 90000 00001");
        user.setRole(Role.DONOR);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        DonorProfile profile = new DonorProfile();
        profile.setUser(saved);
        profile.setBusinessName("Kitchen " + suffix);
        profile.setBusinessType("RESTAURANT");
        profile.setContactPerson("Contact " + suffix);
        profile.setAddressLine1("1 Test Street");
        profile.setCity("Pune");
        profile.setState("Maharashtra");
        profile.setPostalCode("411001");
        donorProfileRepository.save(profile);
        return saved;
    }

    public User ngo(VerificationStatus verification) {
        String suffix = "ngo" + counter.incrementAndGet();
        User user = new User();
        user.setEmail(suffix + "@example.org");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("NGO " + suffix);
        user.setPhone("+91 90000 00002");
        user.setRole(Role.NGO);
        user.setStatus(verification == VerificationStatus.APPROVED ? UserStatus.ACTIVE : UserStatus.PENDING);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        NgoProfile profile = new NgoProfile();
        profile.setUser(saved);
        profile.setOrganizationName("Foundation " + suffix);
        profile.setRegistrationNumber("REG-" + suffix.toUpperCase());
        profile.setContactPerson("Contact " + suffix);
        profile.setAddressLine1("2 Test Street");
        profile.setCity("Pune");
        profile.setState("Maharashtra");
        profile.setPostalCode("411002");
        profile.setVerificationStatus(verification);
        ngoProfileRepository.save(profile);
        return saved;
    }

    public User approvedNgo() {
        return ngo(VerificationStatus.APPROVED);
    }

    public User admin() {
        String suffix = "admin" + counter.incrementAndGet();
        User user = new User();
        user.setEmail(suffix + "@example.org");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Admin " + suffix);
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    public FoodCategory category(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.findAll().get(0));
    }

    public Donation donation(User donor) {
        return donation(donor, Instant.now().plusSeconds(6 * 3600));
    }

    public Donation donation(User donor, Instant expiresAt) {
        FoodCategory category = category("cooked-meals");
        Donation donation = new Donation();
        donation.setReferenceCode("FB-" + String.format("%08d", counter.incrementAndGet()));
        donation.setDonor(donor);
        donation.setCategory(category);
        donation.setFoodName("Vegetable Biryani");
        donation.setDescription("Freshly cooked surplus rice");
        donation.setQuantity(new BigDecimal("8.00"));
        donation.setQuantityUnit("KG");
        donation.setFoodType(FoodType.VEGETARIAN);
        donation.setPreparedAt(Instant.now().minusSeconds(3600));
        donation.setExpiresAt(expiresAt);
        donation.setPickupAddress("1 Test Street, Pune");
        donation.setCity("Pune");
        donation.setPickupInstructions("Ring the bell at the service entrance");
        donation.setStorageConditions("Refrigerated below 5C");
        donation.setAllergenInfo("May contain nuts");
        donation.setHandlingInstructions("Use clean containers");
        donation.setSafetyDeclaration(true);
        donation.setEstimatedMeals(20);
        donation.setStatus(DonationStatus.AVAILABLE);
        return donation;
    }
}
