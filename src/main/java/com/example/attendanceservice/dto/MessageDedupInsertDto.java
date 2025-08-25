package com.example.attendanceservice.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MessageDedupInsertDto(
        UUID messageId,
        UUID eventId,
        String consumerGroup,
        String topic,
        int partitionNo,
        Long offsetNo,
        String message,
        String producerApp
) {
}
