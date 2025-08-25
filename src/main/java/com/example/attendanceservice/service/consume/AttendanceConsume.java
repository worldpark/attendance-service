package com.example.attendanceservice.service.consume;

import com.example.attendanceservice.dto.AttendanceCheckEvent;
import com.example.attendanceservice.dto.MessageDedupInsertDto;
import com.example.attendanceservice.entity.Attendance;
import com.example.attendanceservice.entity.User;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.example.attendanceservice.security.JwtSecurity;
import com.example.attendanceservice.service.MessageDedupService;
import com.example.attendanceservice.util.UUIDv6Generator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceConsume {

    private final AttendanceRepository attendanceRepository;
    private final MessageDedupService messageDedupService;
    private final JwtSecurity jwtSecurity;

    @KafkaListener(
            topics = "outbox.event.attendance",
            groupId = "attendance-service",
            concurrency = "2"
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    public void attendanceCheck(
            @Header("id") String eventId,
            @Header("jwt") String jwt,
            String message,
            ConsumerRecord<String, String> record){

        UUID uuEventId = UUID.fromString(eventId);

        try {
            AttendanceCheckEvent attendanceCheckEvent = AttendanceCheckEvent.fromJson(message);

            if(jwtSecurity.checkingJwt(jwt)){

                MessageDedupInsertDto messageDedupInsertDto = MessageDedupInsertDto.builder()
                        .messageId(UUIDv6Generator.generate())
                        .eventId(uuEventId)
                        .consumerGroup("attendance-service")
                        .topic(record.topic())
                        .partitionNo(record.partition())
                        .offsetNo(record.offset())
                        .message(message)
                        .producerApp("attendance-service")
                        .build();

                boolean duplicateCheck = messageDedupService.duplicateCheck(messageDedupInsertDto);

                if(!duplicateCheck){
                    return;
                }

                User user = User.builder()
                        .userId(attendanceCheckEvent.getUserId())
                        .build();

                Attendance attendance = Attendance.builder()
                        .attendanceId(attendanceCheckEvent.getAttendanceId())
                        .attendanceDate(attendanceCheckEvent.getAttendanceDate())
                        .checkTime(attendanceCheckEvent.getCheckTime())
                        .status(attendanceCheckEvent.getStatus())
                        .memo(attendanceCheckEvent.getMemo())
                        .user(user)
                        .build();

                attendanceRepository.save(attendance);
                messageDedupService.markProcessed(messageDedupInsertDto);
            }
        }catch (Exception exception){
            messageDedupService.markFailed(uuEventId, "attendance-service", exception);
            throw exception;
        }

    }
}
