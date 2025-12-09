package com.perepilka.coreservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Client for communicating with the internal Telegram Bot Service.
 * 
 * Implements: FR-CORE-05 (Send notifications to Telegram users)
 * Target: POST /internal/send endpoint on Telegram Bot Service
 */
@Slf4j
@Service
public class TelegramServiceClient {

    private final RestClient restClient;
    private final String telegramServiceUrl;
    private final String internalServiceKey;

    /**
     * Constructor with dependency injection.
     *
     * @param restClientBuilder RestClient.Builder for creating HTTP client
     * @param telegramServiceUrl Base URL of Telegram Bot Service
     * @param internalServiceKey Shared secret for authentication
     */
    public TelegramServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${telegram.service.url}") String telegramServiceUrl,
            @Value("${internal.service.key}") String internalServiceKey) {
        
        this.telegramServiceUrl = telegramServiceUrl;
        this.internalServiceKey = internalServiceKey;
        
        // Create RestClient with base configuration
        this.restClient = restClientBuilder
                .baseUrl(telegramServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Service-Key", internalServiceKey)
                .build();
        
        log.info("TelegramServiceClient initialized: url={}", telegramServiceUrl);
    }

    /**
     * Send a notification to a Telegram user.
     * 
     * Implements: POST {telegram.service.url}/internal/send
     * 
     * @param chatId Telegram chat ID (as string)
     * @param message Message text to send (supports HTML formatting)
     * 
     * Example:
     *   sendNotification("123456789", "🔴 MrBeast is live!\n\nLink: https://youtu.be/xyz");
     */
    public void sendNotification(String chatId, String message) {
        if (chatId == null || chatId.isBlank()) {
            log.warn("Cannot send notification: chatId is null or empty");
            return;
        }
        
        if (message == null || message.isBlank()) {
            log.warn("Cannot send notification to chatId={}: message is null or empty", chatId);
            return;
        }
        
        try {
            // Prepare request body
            Map<String, String> requestBody = Map.of(
                    "chatId", chatId,
                    "message", message
            );
            
            log.debug("Sending notification to chatId={}: message_length={}", 
                    chatId, message.length());
            
            // Send POST request to /internal/send
            String response = restClient.post()
                    .uri("/internal/send")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            
            log.info("Notification sent successfully to chatId={}", chatId);
            log.debug("Telegram service response: {}", response);
            
        } catch (RestClientException e) {
            // Log error but don't crash the application
            log.error(
                    "Failed to send notification to chatId={}: {} - {}",
                    chatId,
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            
            // Log full stack trace at debug level
            log.debug("Full exception details:", e);
            
        } catch (Exception e) {
            // Catch any unexpected exceptions
            log.error(
                    "Unexpected error sending notification to chatId={}: {}",
                    chatId,
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Send a notification with formatted channel information.
     * 
     * Convenience method for stream notifications.
     *
     * @param chatId Telegram chat ID
     * @param platform Platform name (YOUTUBE, TWITCH, etc.)
     * @param channelName Channel name/username
     * @param channelUrl URL to the live stream or channel
     */
    public void sendStreamNotification(
            String chatId,
            String platform,
            String channelName,
            String channelUrl) {
        
        // Build formatted message with HTML
        String platformEmoji = getPlatformEmoji(platform);
        
        String message = String.format(
                "%s <b>%s is live!</b>\n\n" +
                "Platform: <b>%s</b>\n" +
                "Channel: <b>%s</b>\n\n" +
                "🔗 <a href='%s'>Watch Now</a>",
                platformEmoji,
                channelName,
                platform,
                channelName,
                channelUrl
        );
        
        sendNotification(chatId, message);
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

    /**
     * Test connection to Telegram Bot Service.
     * 
     * @return true if service is reachable, false otherwise
     */
    public boolean testConnection() {
        try {
            log.info("Testing connection to Telegram Bot Service...");
            
            String response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            
            log.info("Telegram Bot Service is reachable: {}", response);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to connect to Telegram Bot Service: {}", e.getMessage());
            return false;
        }
    }
}
