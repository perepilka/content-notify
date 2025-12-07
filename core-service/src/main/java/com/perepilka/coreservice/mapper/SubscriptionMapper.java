package com.perepilka.coreservice.mapper;

import com.perepilka.coreservice.domain.Subscription;
import com.perepilka.coreservice.dto.SubscriptionResponse;

public class SubscriptionMapper {

    private SubscriptionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SubscriptionResponse toDto(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .platform(subscription.getPlatform().name())
                .channelUrl(subscription.getChannelUrl())
                .channelName(subscription.getChannelName())
                .build();
    }
}
