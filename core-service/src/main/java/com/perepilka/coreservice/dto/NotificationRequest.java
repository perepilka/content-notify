package com.perepilka.coreservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for triggering stream notifications.
 * 
 * Used by external stream monitoring services to notify subscribers
 * when a channel goes live.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /**
     * The channel URL that went live.
     * Must match a subscription's channelUrl.
     * 
     * Example: "https://www.youtube.com/@MrBeast"
     */
    @NotBlank(message = "Channel URL is required")
    private String channelUrl;

    /**
     * The title of the live stream.
     * 
     * Example: "I Survived 50 Hours In Antarctica"
     */
    @NotBlank(message = "Stream title is required")
    private String streamTitle;

    /**
     * Direct URL to the live stream.
     * 
     * Example: "https://www.youtube.com/watch?v=abc123"
     */
    @NotBlank(message = "Stream URL is required")
    private String streamUrl;
}
