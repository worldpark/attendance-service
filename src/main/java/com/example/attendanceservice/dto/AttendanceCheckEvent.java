package com.example.attendanceservice.dto;

import com.example.attendanceservice.enumeratedType.AttendanceStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class AttendanceCheckEvent {

    private UUID attendanceId;
    private LocalDateTime attendanceDate;
    private LocalDateTime checkTime;
    private AttendanceStatus status;
    private String memo;
    private UUID userId;

    public static AttendanceCheckEvent fromJson(String json){

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            JsonNode root = objectMapper.readTree(json);
            JsonNode payload = root.get("payload");

            if (payload == null || payload.isNull()) {
                throw new IllegalArgumentException("payload is required");
            }

            if (payload.isTextual()) {
                return objectMapper.readValue(payload.asText(), AttendanceCheckEvent.class);
            }

            return objectMapper.treeToValue(payload, AttendanceCheckEvent.class);
        }catch (JsonProcessingException exception){
            throw new RuntimeException(exception);
        }
    }
}
