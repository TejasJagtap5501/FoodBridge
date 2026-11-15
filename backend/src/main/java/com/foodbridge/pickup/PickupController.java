package com.foodbridge.pickup;

import com.foodbridge.pickup.dto.PickupResponse;
import com.foodbridge.pickup.dto.ReschedulePickupRequest;
import com.foodbridge.pickup.dto.SchedulePickupRequest;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pickups")
@RequiredArgsConstructor
@Tag(name = "Pickups", description = "Coordinate and confirm food pickups")
public class PickupController {

    private final PickupService pickupService;

    @PostMapping("/claims/{claimId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule (or reschedule) the pickup for a claim")
    public PickupResponse schedule(@PathVariable Long claimId,
                                   @Valid @RequestBody SchedulePickupRequest request) {
        return pickupService.schedule(CurrentUser.require(), claimId, request);
    }

    @GetMapping
    @Operation(summary = "List my pickups (donor or NGO)")
    public List<PickupResponse> mine() {
        return pickupService.myPickups(CurrentUser.require());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pickup details")
    public PickupResponse get(@PathVariable Long id) {
        return pickupService.getPickup(CurrentUser.require(), id);
    }

    @PutMapping("/{id}/schedule")
    @Operation(summary = "Reschedule a pending pickup")
    public PickupResponse reschedule(@PathVariable Long id, @Valid @RequestBody ReschedulePickupRequest request) {
        return pickupService.reschedule(CurrentUser.require(), id, request);
    }

    @PostMapping("/{id}/confirm-handover")
    @Operation(summary = "Donor confirms the food was handed over")
    public PickupResponse confirmHandover(@PathVariable Long id) {
        return pickupService.confirmHandover(CurrentUser.require(), id);
    }

    @PostMapping("/{id}/confirm-collection")
    @Operation(summary = "NGO confirms the food was collected")
    public PickupResponse confirmCollection(@PathVariable Long id) {
        return pickupService.confirmCollection(CurrentUser.require(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pickup (also cancels the claim)")
    public PickupResponse cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Cancelled by user");
        return pickupService.cancel(CurrentUser.require(), id, reason);
    }
}
