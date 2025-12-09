package com.perepilka.coreservice.controller;

import com.perepilka.coreservice.dto.ErrorResponse;
import com.perepilka.coreservice.dto.NotificationRequest;
import com.perepilka.coreservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for handling stream notification triggers.
 * 
 * Implements: FR-CORE-05 (Notify users when channels go live)
 * 
 * This endpoint is called by external stream monitoring services
 * when they detect a channel has gone live.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Stream notification API")
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    /**
     * Trigger notifications for a live stream.
     * 
     * This endpoint is secured with X-Internal-Service-Key header.
     * Only authorized services (e.g., stream monitoring service) can call it.
     * 
     * @param authHeader X-Internal-Service-Key header for authentication
     * @param request Notification request with channel and stream details
     * @return Success response or error
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "Trigger stream notifications",
        description = "Notify all subscribers when a channel goes live. Requires X-Internal-Service-Key header."
    )
    public ResponseEntity<?> triggerNotification(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String authHeader,
            @Valid @RequestBody NotificationRequest request) {

        log.info(
            "Received notification trigger: channelUrl={}, streamTitle={}",
            request.getChannelUrl(),
            request.getStreamTitle()
        );

        // Validate internal service key
        if (authHeader == null || authHeader.isBlank()) {
            log.warn("Notification trigger rejected: Missing X-Internal-Service-Key header");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.FORBIDDEN.value())
                            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                            .message("Missing authentication header")
                            .path("/api/v1/notifications/trigger")
                            .build());
        }

        if (!authHeader.equals(internalServiceKey)) {
            log.warn(
                "Notification trigger rejected: Invalid X-Internal-Service-Key header"
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.FORBIDDEN.value())
                            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                            .message("Invalid authentication key")
                            .path("/api/v1/notifications/trigger")
                            .build());
        }

        // Process notification
        try {
            notificationService.processNotification(request);

            log.info(
                "Notification trigger processed successfully: channelUrl={}",
                request.getChannelUrl()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Notifications sent to subscribers",
                    "channelUrl", request.getChannelUrl()
            ));

        } catch (Exception e) {
            log.error(
                "Failed to process notification trigger: channelUrl={}, error={}",
                request.getChannelUrl(),
                e.getMessage(),
                e
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                            .message("Failed to process notification: " + e.getMessage())
                            .path("/api/v1/notifications/trigger")
                            .build());
        }
    }
}
