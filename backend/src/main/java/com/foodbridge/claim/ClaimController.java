package com.foodbridge.claim;

import com.foodbridge.claim.dto.CancelClaimRequest;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.claim.dto.ClaimStatsResponse;
import com.foodbridge.common.PageResponse;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim donations and manage claim lifecycle")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/donations/{donationId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Claim an available donation (verified NGO only)")
    public ClaimResponse claim(@PathVariable Long donationId, @Valid @RequestBody ClaimRequest request) {
        return claimService.claim(CurrentUser.require(), donationId, request);
    }

    @GetMapping
    @Operation(summary = "List my claims (scope depends on role)")
    public PageResponse<ClaimResponse> mine(@RequestParam(required = false) ClaimStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "claimedAt") String sortBy,
                                            @RequestParam(defaultValue = "desc") String direction) {
        return claimService.myClaims(CurrentUser.require(), status, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Claim details (donor, claiming NGO or admin only)")
    public ClaimResponse get(@PathVariable Long id) {
        return claimService.getClaim(CurrentUser.require(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an active claim")
    public ClaimResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelClaimRequest request) {
        return claimService.cancel(CurrentUser.require(), id, request);
    }

    @GetMapping("/stats")
    @Operation(summary = "Claim counters for the current NGO")
    public ClaimStatsResponse stats() {
        return claimService.statsForNgo(CurrentUser.requireId());
    }
}
