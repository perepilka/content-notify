package com.perepilka.coreservice.controller;

import com.perepilka.coreservice.dto.SubscriptionRequest;
import com.perepilka.coreservice.dto.SubscriptionResponse;
import com.perepilka.coreservice.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management API")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Add a new subscription.
     * FR-CORE-02: Add subscription with URL validation.
     *
     * @param request subscription request containing accountId and URL
     * @return created subscription response
     */
    @PostMapping
    @Operation(summary = "Add subscription", description = "Create a new subscription for a YouTube or Twitch channel")
    public ResponseEntity<SubscriptionResponse> addSubscription(@Valid @RequestBody SubscriptionRequest request) {
        log.info("Received add subscription request: accountId={}, url={}", 
                request.getAccountId(), request.getUrl());
        
        SubscriptionResponse response = subscriptionService.addSubscription(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all subscriptions for a user.
     * FR-CORE-03: List user subscriptions.
     *
     * @param accountId account UUID
     * @return list of subscriptions
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get user subscriptions", description = "Retrieve all subscriptions for a specific account")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions(@PathVariable UUID accountId) {
        log.info("Received get subscriptions request: accountId={}", accountId);
        
        List<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions(accountId);
        
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Remove a subscription.
     * FR-CORE-04: Delete subscription with ownership validation.
     *
     * @param id subscription ID
     * @param accountId account UUID for ownership validation
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subscription", description = "Remove a subscription by ID (requires account ownership)")
    public ResponseEntity<Void> removeSubscription(
            @PathVariable Long id,
            @RequestParam UUID accountId) {
        log.info("Received delete subscription request: id={}, accountId={}", id, accountId);
        
        subscriptionService.removeSubscription(id, accountId);
        
        return ResponseEntity.noContent().build();
    }
}
