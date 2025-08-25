package com.example.attendanceservice.entity;

import com.example.attendanceservice.util.UUIDv6Generator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "message_dedup",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_group", columnNames = {"event_id", "consumer_group"}),
                @UniqueConstraint(name = "uk_coord", columnNames = {"topic", "partition_no", "offset_no"})
        },
        indexes = {
                @Index(name = "idx_status_processed_at", columnList = "status, processed_at"),
                @Index(name = "idx_first_seen", columnList = "first_seen_at")
        }
)
@Builder
public class MessageDedup {

    @Id
    private UUID messageId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 128)
    private String consumerGroup;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column( nullable = false)
    private Integer partitionNo;

    @Column(nullable = false)
    private Long offsetNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(length = 128)
    private String errorCode;

    @Lob
    @Column
    private String errorMessage;

    @Column( columnDefinition = "BINARY(32)")
    private byte[] payloadHash;

    @Column(length = 128)
    private String producerApp;

    public enum Status { RECEIVED, PROCESSED, FAILED }

    @PrePersist
    public void prePersist() {
        if (messageId == null) {
            messageId = UUIDv6Generator.generate();
        }
    }
}
