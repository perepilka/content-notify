package com.perepilka.coreservice.service;

import com.perepilka.coreservice.domain.Account;
import com.perepilka.coreservice.domain.Platform;
import com.perepilka.coreservice.domain.Subscription;
import com.perepilka.coreservice.dto.SubscriptionRequest;
import com.perepilka.coreservice.dto.SubscriptionResponse;
import com.perepilka.coreservice.exception.DuplicateSubscriptionException;
import com.perepilka.coreservice.exception.InvalidUrlException;
import com.perepilka.coreservice.exception.ResourceNotFoundException;
import com.perepilka.coreservice.mapper.SubscriptionMapper;
import com.perepilka.coreservice.repository.AccountRepository;
import com.perepilka.coreservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile("^https?://(www\\.)?youtube\\.com/@[\\w-]+$");
    private static final Pattern TWITCH_PATTERN = Pattern.compile("^https?://(www\\.)?twitch\\.tv/[\\w-]+$");

    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;

    /**
     * Add a new subscription.
     * FR-CORE-02: Add subscription endpoint with URL validation.
     *
     * @param request subscription request containing accountId and URL
     * @return created subscription response
     */
    @Transactional
    public SubscriptionResponse addSubscription(SubscriptionRequest request) {
        UUID accountId = request.getAccountId();
        String url = request.getUrl();

        log.info("Adding subscription: accountId={}, url={}", accountId, url);

        // Check if account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Validate URL and detect platform
        Platform platform = detectPlatform(url);

        // Check for duplicates
        if (subscriptionRepository.existsByAccountIdAndChannelUrl(accountId, url)) {
            throw new DuplicateSubscriptionException("Subscription already exists for this channel");
        }

        // Create and save subscription
        Subscription subscription = Subscription.builder()
                .account(account)
                .platform(platform)
                .channelUrl(url)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription created: id={}, platform={}", savedSubscription.getId(), platform);

        return SubscriptionMapper.toDto(savedSubscription);
    }

    /**
     * Get all subscriptions for a user.
     * FR-CORE-03: List subscriptions endpoint.
     *
     * @param accountId account UUID
     * @return list of subscription responses
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAllSubscriptions(UUID accountId) {
        log.info("Retrieving all subscriptions for accountId={}", accountId);

        List<Subscription> subscriptions = subscriptionRepository.findByAccountId(accountId);
        
        return subscriptions.stream()
                .map(SubscriptionMapper::toDto)
                .toList();
    }

    /**
     * Remove a subscription.
     * FR-CORE-04: Delete subscription endpoint with ownership validation.
     *
     * @param subscriptionId subscription ID
     * @param accountId account UUID for ownership validation
     */
    @Transactional
    public void removeSubscription(Long subscriptionId, UUID accountId) {
        log.info("Removing subscription: id={}, accountId={}", subscriptionId, accountId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));

        // Verify ownership
        if (!subscription.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Subscription not found or access denied");
        }

        subscriptionRepository.delete(subscription);
        log.info("Subscription removed: id={}", subscriptionId);
    }

    /**
     * Detect platform from URL using regex validation.
     * Section 6.1: Input Validation.
     *
     * @param url channel URL
     * @return detected platform
     * @throws InvalidUrlException if URL format is invalid
     */
    private Platform detectPlatform(String url) {
        if (YOUTUBE_PATTERN.matcher(url).matches()) {
            return Platform.YOUTUBE;
        } else if (TWITCH_PATTERN.matcher(url).matches()) {
            return Platform.TWITCH;
        } else {
            throw new InvalidUrlException("Invalid URL format. Expected YouTube (@username) or Twitch (/username) URL");
        }
    }
}
