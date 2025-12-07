package com.perepilka.coreservice.repository;

import com.perepilka.coreservice.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find all subscriptions for a given account.
     * Used for listing user subscriptions (FR-CORE-03).
     *
     * @param accountId the account UUID
     * @return list of subscriptions
     */
    List<Subscription> findByAccountId(UUID accountId);

    /**
     * Find a subscription by account ID and channel URL.
     * Used to check for duplicates before adding a subscription.
     *
     * @param accountId the account UUID
     * @param channelUrl the channel URL
     * @return Optional containing the subscription if found
     */
    Optional<Subscription> findByAccountIdAndChannelUrl(UUID accountId, String channelUrl);

    /**
     * Check if a subscription exists for a given account and channel URL.
     *
     * @param accountId the account UUID
     * @param channelUrl the channel URL
     * @return true if subscription exists, false otherwise
     */
    boolean existsByAccountIdAndChannelUrl(UUID accountId, String channelUrl);

    /**
     * Delete a subscription by ID and account ID.
     * Ensures users can only delete their own subscriptions (FR-CORE-04).
     *
     * @param id the subscription ID
     * @param accountId the account UUID
     */
    void deleteByIdAndAccountId(Long id, UUID accountId);
}
