package com.perepilka.coreservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "channel_url"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "channel_url", nullable = false, length = 500)
    private String channelUrl;

    @Column(name = "channel_name", length = 255)
    private String channelName;
}
