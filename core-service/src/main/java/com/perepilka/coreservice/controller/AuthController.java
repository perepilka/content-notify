package com.perepilka.coreservice.controller;

import com.perepilka.coreservice.dto.AuthRequest;
import com.perepilka.coreservice.dto.AuthResponse;
import com.perepilka.coreservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration API")
public class AuthController {

    private final AuthService authService;

    /**
     * Register or retrieve a user based on provider ID.
     * FR-CORE-01: Authentication endpoint.
     *
     * @param request authentication request containing provider and providerId
     * @return authentication response with accountId and isNew flag
     */
    @PostMapping("/auth")
    @Operation(summary = "Authenticate user", description = "Register new user or retrieve existing user by provider ID")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        log.info("Received authentication request: provider={}, providerId={}", 
                request.getProvider(), request.getProviderId());
        
        AuthResponse response = authService.authenticate(request);
        
        return ResponseEntity.ok(response);
    }
}
