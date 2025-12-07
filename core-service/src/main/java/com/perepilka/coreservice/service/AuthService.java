package com.perepilka.coreservice.service;

import com.perepilka.coreservice.domain.Account;
import com.perepilka.coreservice.domain.Connection;
import com.perepilka.coreservice.domain.Provider;
import com.perepilka.coreservice.dto.AuthRequest;
import com.perepilka.coreservice.dto.AuthResponse;
import com.perepilka.coreservice.repository.AccountRepository;
import com.perepilka.coreservice.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;

    /**
     * Register or retrieve a user based on provider and provider ID.
     * FR-CORE-01: Authentication endpoint.
     *
     * @param request authentication request containing provider and providerId
     * @return authentication response with accountId and isNew flag
     */
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        Provider provider = Provider.valueOf(request.getProvider().toUpperCase());
        String providerId = request.getProviderId();

        log.info("Authenticating user: provider={}, providerId={}", provider, providerId);

        Optional<Connection> existingConnection = connectionRepository
                .findByProviderAndProviderId(provider, providerId);

        if (existingConnection.isPresent()) {
            log.info("Existing user found: accountId={}", existingConnection.get().getAccount().getId());
            return AuthResponse.builder()
                    .accountId(existingConnection.get().getAccount().getId())
                    .isNew(false)
                    .build();
        }

        Account newAccount = Account.builder().build();
        Account savedAccount = accountRepository.save(newAccount);
        log.info("New account created: accountId={}", savedAccount.getId());

        Connection newConnection = Connection.builder()
                .account(savedAccount)
                .provider(provider)
                .providerId(providerId)
                .build();
        connectionRepository.save(newConnection);
        log.info("New connection created: provider={}, providerId={}", provider, providerId);

        return AuthResponse.builder()
                .accountId(savedAccount.getId())
                .isNew(true)
                .build();
    }
}
