package com.foodbridge.donation;

import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.dto.*;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Donation endpoints shared by donors and NGOs. Role-specific prefixes
 * ({@code /api/donor}, {@code /api/ngo}) are enforced in
 * {@link com.foodbridge.config.SecurityConfig}; ownership is re-checked inside
 * {@link DonationService}.
 */
@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Publish, browse and manage food donations")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a new food donation (DONOR only)")
    public DonationResponse create(@Valid @RequestBody DonationCreateRequest request) {
        return donationService.create(CurrentUser.require(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donation details")
    public DonationResponse get(@PathVariable Long id) {
        return donationService.getDonation(CurrentUser.require(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an available donation (owner only)")
    public DonationResponse update(@PathVariable Long id, @Valid @RequestBody DonationUpdateRequest request) {
        return donationService.update(CurrentUser.require(), id, request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an available donation (owner only)")
    public DonationResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelDonationRequest request) {
        return donationService.cancelByDonor(CurrentUser.require(), id, request.reason());
    }

    @GetMapping("/mine")
    @Operation(summary = "List my donations with search, filtering, sorting and pagination (DONOR only)")
    public PageResponse<DonationSummaryResponse> mine(@RequestParam(required = false) DonationStatus status,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(required = false) String city,
                                                      @RequestParam(required = false) Instant expiringBefore,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "createdAt") String sortBy,
                                                      @RequestParam(defaultValue = "desc") String direction) {
        return donationService.listForDonor(CurrentUser.require(), status, categoryId, search, city,
                expiringBefore, page, size, sortBy, direction);
    }

    @GetMapping("/available")
    @Operation(summary = "Browse available donations with filters (NGO only)")
    public PageResponse<DonationSummaryResponse> available(@RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) String city,
                                                           @RequestParam(required = false) java.math.BigDecimal minQuantity,
                                                           @RequestParam(required = false) Instant expiringBefore,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "12") int size,
                                                           @RequestParam(defaultValue = "expiresAt") String sortBy,
                                                           @RequestParam(defaultValue = "asc") String direction) {
        return donationService.browseAvailable(categoryId, search, city, minQuantity, expiringBefore,
                page, size, sortBy, direction);
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard counters for the current donor")
    public DonationStatsResponse stats() {
        return donationService.statsForDonor(CurrentUser.requireId());
    }
}
