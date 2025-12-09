package com.perepilka.coreservice.service;

import com.perepilka.coreservice.client.TelegramServiceClient;
import com.perepilka.coreservice.domain.Connection;
import com.perepilka.coreservice.domain.Provider;
import com.perepilka.coreservice.domain.Subscription;
import com.perepilka.coreservice.dto.NotificationRequest;
import com.perepilka.coreservice.repository.ConnectionRepository;
import com.perepilka.coreservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for processing and sending stream notifications.
 * 
 * Implements: FR-CORE-05 (Notify users when channels go live)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SubscriptionRepository subscriptionRepository;
    private final ConnectionRepository connectionRepository;
    private final TelegramServiceClient telegramServiceClient;

    /**
     * Process a notification request when a channel goes live.
     * 
     * Workflow:
     * 1. Find all subscriptions for the channel URL
     * 2. For each subscription, get the user's Telegram connection
     * 3. Send notification via TelegramServiceClient
     * 
     * @param request Notification request with channel and stream details
     */
    @Transactional(readOnly = true)
    public void processNotification(NotificationRequest request) {
        log.info(
            "Processing notification: channelUrl={}, streamTitle={}",
            request.getChannelUrl(),
            request.getStreamTitle()
        );

        // Find all subscribers for this channel
        List<Subscription> subscriptions = subscriptionRepository
                .findAllByChannelUrl(request.getChannelUrl());

        if (subscriptions.isEmpty()) {
            log.info(
                "No subscribers found for channelUrl={}",
                request.getChannelUrl()
            );
            return;
        }

        log.info(
            "Found {} subscriber(s) for channelUrl={}",
            subscriptions.size(),
            request.getChannelUrl()
        );

        int successCount = 0;
        int failureCount = 0;

        // Send notification to each subscriber
        for (Subscription subscription : subscriptions) {
            try {
                // Get user's Telegram connection
                Connection telegramConnection = connectionRepository
                        .findByAccountIdAndProvider(
                            subscription.getAccount().getId(),
                            Provider.TELEGRAM
                        )
                        .orElse(null);

                if (telegramConnection == null) {
                    log.warn(
                        "No Telegram connection found for accountId={}, skipping notification",
                        subscription.getAccount().getId()
                    );
                    failureCount++;
                    continue;
                }

                // Build notification message
                String message = buildNotificationMessage(
                    request.getStreamTitle(),
                    subscription.getPlatform().name(),
                    request.getStreamUrl()
                );

                // Send notification via Telegram
                telegramServiceClient.sendNotification(
                    telegramConnection.getProviderId(),
                    message
                );

                successCount++;
                log.debug(
                    "Notification sent to accountId={}, chatId={}",
                    subscription.getAccount().getId(),
                    telegramConnection.getProviderId()
                );

            } catch (Exception e) {
                failureCount++;
                log.error(
                    "Failed to send notification for accountId={}: {}",
                    subscription.getAccount().getId(),
                    e.getMessage(),
                    e
                );
            }
        }

        log.info(
            "Notification processing completed: channelUrl={}, total={}, success={}, failure={}",
            request.getChannelUrl(),
            subscriptions.size(),
            successCount,
            failureCount
        );
    }

    /**
     * Build formatted notification message.
     * 
     * @param streamTitle Title of the stream
     * @param platform Platform name (YOUTUBE, TWITCH)
     * @param streamUrl Direct URL to the stream
     * @return Formatted HTML message
     */
    private String buildNotificationMessage(
            String streamTitle,
            String platform,
            String streamUrl) {
        
        String platformEmoji = getPlatformEmoji(platform);
        
        return String.format(
            "%s <b>%s</b> is live!\n\n" +
            "Platform: <b>%s</b>\n" +
            "Title: <i>%s</i>\n\n" +
            "🔗 <a href='%s'>Watch Now</a>",
            platformEmoji,
            extractChannelName(streamUrl),
            platform,
            streamTitle,
            streamUrl
        );
    }

    /**
     * Extract channel name from stream URL.
     * 
     * @param streamUrl Stream URL
     * @return Channel name or "Channel"
     */
    private String extractChannelName(String streamUrl) {
        if (streamUrl.contains("youtube.com/@")) {
            int start = streamUrl.indexOf("/@") + 2;
            int end = streamUrl.indexOf("/", start);
            if (end > 0) {
                return streamUrl.substring(start, end);
            }
            return streamUrl.substring(start);
        }
        if (streamUrl.contains("twitch.tv/")) {
            int start = streamUrl.indexOf("twitch.tv/") + 10;
            int end = streamUrl.indexOf("/", start);
            if (end > 0) {
                return streamUrl.substring(start, end);
            }
            return streamUrl.substring(start);
        }
        return "Channel";
    }

    /**
     * Get emoji for platform.
     * 
     * @param platform Platform name
     * @return Platform emoji
     */
    private String getPlatformEmoji(String platform) {
        return switch (platform.toUpperCase()) {
            case "YOUTUBE" -> "📺";
            case "TWITCH" -> "🎮";
            default -> "🔴";
        };
    }
}
