package com.example.attendanceservice.service;

import com.example.attendanceservice.dto.MessageDedupInsertDto;
import com.example.attendanceservice.entity.MessageDedup;
import com.example.attendanceservice.repository.MessageDedupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageDedupService {

    private final MessageDedupRepository messageDedupRepository;

    @Transactional(readOnly = false)
    public boolean duplicateCheck(MessageDedupInsertDto messageDedupInsertDto){

        int duplicateCheck = messageDedupRepository.checkMessageData(
                messageDedupInsertDto.messageId(),
                messageDedupInsertDto.eventId(),
                messageDedupInsertDto.consumerGroup(),
                messageDedupInsertDto.topic(),
                messageDedupInsertDto.partitionNo(),
                messageDedupInsertDto.offsetNo(),
                sha256(messageDedupInsertDto.message()),
                messageDedupInsertDto.producerApp()
        );

        return duplicateCheck == 1;
    }

    @Transactional(readOnly = false)
    public void markProcessed(MessageDedupInsertDto messageDedupInsertDto){

        messageDedupRepository.markProcessed(
                messageDedupInsertDto.eventId(),
                messageDedupInsertDto.consumerGroup(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = false)
    public void markFailed(UUID eventId, String consumerGroup, Exception exception){

        String message = exception.getMessage();

        if (message != null && message.length() > 2000) {
            message = message.substring(0, 2000);
        }

        messageDedupRepository.markFailed(
                eventId,
                consumerGroup,
                exception.getClass().getSimpleName(),
                message);
    }

    private byte[] sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
