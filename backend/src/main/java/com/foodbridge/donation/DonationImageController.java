package com.foodbridge.donation;

import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/donations/{donationId}/images")
@RequiredArgsConstructor
@Tag(name = "Donation Images", description = "Upload photos of donated food")
public class DonationImageController {

    private final DonationImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo for a donation (owner only, max 3 images, 5 MB each)")
    public Map<String, String> upload(@PathVariable Long donationId, @RequestParam("file") MultipartFile file) {
        String url = imageService.store(CurrentUser.require(), donationId, file);
        return Map.of("url", url);
    }
}
