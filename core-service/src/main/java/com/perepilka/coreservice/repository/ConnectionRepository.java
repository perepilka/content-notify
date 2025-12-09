package com.perepilka.coreservice.repository;

import com.perepilka.coreservice.domain.Connection;
import com.perepilka.coreservice.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    /**
     * Find a connection by provider and provider ID.
     * Used for authentication (FR-CORE-01).
     *
     * @param provider the provider type (e.g., TELEGRAM)
     * @param providerId the external provider's user ID
     * @return Optional containing the connection if found
     */
    Optional<Connection> findByProviderAndProviderId(Provider provider, String providerId);

    /**
     * Check if a connection exists for a given provider and provider ID.
     *
     * @param provider the provider type
     * @param providerId the external provider's user ID
     * @return true if connection exists, false otherwise
     */
    boolean existsByProviderAndProviderId(Provider provider, String providerId);

    /**
     * Find a connection by account ID and provider.
     *
     * @param accountId the account UUID
     * @param provider the provider type (e.g., TELEGRAM)
     * @return Optional containing the connection if found
     */
    Optional<Connection> findByAccountIdAndProvider(UUID accountId, Provider provider);
}
