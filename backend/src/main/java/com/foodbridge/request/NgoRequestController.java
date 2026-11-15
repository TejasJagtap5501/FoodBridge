package com.foodbridge.request;

import com.foodbridge.common.PageResponse;
import com.foodbridge.request.dto.NgoRequestCreate;
import com.foodbridge.request.dto.NgoRequestResponse;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * NGO food requirement requests. Creation/patching is limited to the owning NGO
 * by service-level ownership checks; the open list is readable by any
 * authenticated user.
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Food Requests", description = "NGO food requirement requests")
public class NgoRequestController {

    private final NgoRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a food requirement request (NGO only)")
    public NgoRequestResponse create(@Valid @RequestBody NgoRequestCreate request) {
        return service.create(CurrentUser.require(), request);
    }

    @GetMapping("/mine")
    @Operation(summary = "My organisation's requests")
    public PageResponse<NgoRequestResponse> mine(@RequestParam(required = false) NgoRequestStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return service.mine(CurrentUser.require(), status, page, size);
    }

    @GetMapping("/open")
    @Operation(summary = "Open requests (visible to donors and NGOs)")
    public PageResponse<NgoRequestResponse> open(@RequestParam(required = false) String city,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return service.open(city, page, size);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Fulfil or cancel one of my requests")
    public NgoRequestResponse updateStatus(@PathVariable Long id, @RequestParam NgoRequestStatus status) {
        return service.updateStatus(CurrentUser.require(), id, status);
    }
}
