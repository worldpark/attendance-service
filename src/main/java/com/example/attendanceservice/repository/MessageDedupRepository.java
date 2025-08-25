package com.example.attendanceservice.repository;

import com.example.attendanceservice.entity.MessageDedup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MessageDedupRepository extends JpaRepository<MessageDedup, UUID> {

    @Modifying
    @Query(
            value = """
                INSERT INTO message_dedup
                              (message_id, event_id, consumer_group, topic, partition_no, offset_no,
                               status, first_seen_at, retry_count, payload_hash, producer_app)
                            VALUES
                              (:messageId, :eventId, :consumerGroup, :topic, :partitionNo, :offsetNo,
                               'RECEIVED', NOW(6), 0, :payloadHash, :producerApp)
                            ON DUPLICATE KEY UPDATE message_id = message_id
            """, nativeQuery = true
    )
    int checkMessageData(
            UUID messageId,
            UUID eventId,
            String consumerGroup,
            String topic,
            int partitionNo,
            long offsetNo,
            byte[] payloadHash,
            String producerApp
    );

    @Modifying
    @Query("""
        UPDATE MessageDedup m SET 
            m.status = com.example.attendanceservice.entity.MessageDedup.Status.PROCESSED,
            m.processedAt = :now
        WHERE m.eventId = :eventId 
            AND m.consumerGroup = :consumerGroup
        """)
    int markProcessed(UUID eventId,
                      String consumerGroup,
                      LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE MessageDedup m SET 
            m.status = com.example.attendanceservice.entity.MessageDedup.Status.FAILED,
            m.retryCount = m.retryCount + 1,
            m.errorCode = :errorCode,
            m.errorMessage = :errorMessage,
            m.processedAt = NULL
        WHERE m.eventId = :eventId 
            AND m.consumerGroup = :consumerGroup
        """)
    int markFailed(UUID eventId,
                   String consumerGroup,
                   String errorCode,
                   String errorMessage);
}
