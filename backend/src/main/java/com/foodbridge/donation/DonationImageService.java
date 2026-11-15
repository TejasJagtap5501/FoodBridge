package com.foodbridge.donation;

import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.exception.ApiException;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Donation photo uploads.
 *
 * <p>Safety measures: MIME type allow-list, size limit, magic-byte sniffing,
 * generated storage keys (never the client filename), and storage outside the
 * served directory structure so nothing executable can be requested.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonationImageService {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final FoodBridgeProperties properties;
    private final DonationRepository donationRepository;

    @Transactional
    public String store(AuthenticatedUser actor, Long donationId, MultipartFile file) {
        Donation donation = donationRepository.findDetailedById(donationId)
                .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
        if (!donation.getDonor().getId().equals(actor.id()) && actor.role() != Role.ADMIN) {
            throw ApiException.forbidden("ACCESS_DENIED", "You can only add photos to your own donations");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Please choose a file to upload");
        }
        if (file.getSize() > properties.getUploads().getMaxBytes()) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Images must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.getUploads().allowedTypeList().contains(contentType)) {
            throw ApiException.badRequest("UNSUPPORTED_FILE_TYPE", "Only JPEG, PNG and WebP images are accepted");
        }
        verifyMagicBytes(file);

        if (donation.getImages().size() >= 3) {
            throw ApiException.badRequest("TOO_MANY_IMAGES", "A donation can have at most 3 photos");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String storageKey = "donations/" + donationId + "/" + UUID.randomUUID() + extension;
        Path target = Paths.get(properties.getUploads().getDirectory()).toAbsolutePath().normalize()
                .resolve(storageKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Failed to store donation image for donation {}", donationId, ex);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "UPLOAD_FAILED", "The image could not be stored. Please try again.");
        }

        DonationImage image = new DonationImage();
        image.setStorageKey(storageKey);
        image.setOriginalFilename(sanitizeFilename(file.getOriginalFilename()));
        image.setContentType(contentType);
        image.setSizeBytes(file.getSize());
        donation.addImage(image);
        donationRepository.save(donation);
        log.info("Stored image {} for donation {}", storageKey, donationId);
        return "/uploads/" + storageKey;
    }

    @Transactional(readOnly = true)
    public List<String> imagesFor(Long donationId) {
        return donationRepository.findById(donationId)
                .map(d -> d.getImages().stream().map(i -> "/uploads/" + i.getStorageKey()).toList())
                .orElse(List.of());
    }

    private void verifyMagicBytes(MultipartFile file) {
        byte[] head = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) {
                throw ApiException.badRequest("INVALID_IMAGE", "The uploaded file is not a valid image");
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("INVALID_IMAGE", "The uploaded file could not be read");
        }
        boolean png = startsWith(head, PNG_MAGIC);
        boolean jpeg = startsWith(head, JPEG_MAGIC);
        // WebP: "RIFF"...."WEBP"
        boolean webp = head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F';
        if (!png && !jpeg && !webp) {
            throw ApiException.badRequest("INVALID_IMAGE", "The uploaded file is not a valid JPEG, PNG or WebP image");
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sanitizeFilename(String name) {
        if (name == null) {
            return "upload";
        }
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }
}
