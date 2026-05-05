package com.example.attendanceservice.dto;

import com.example.attendanceservice.enumeratedType.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceCheckEventTest {

    @Test
    void fromJsonReadsObjectPayload() {
        UUID attendanceId = UUID.fromString("018f5f8d-7a63-7000-8000-123456789abc");
        UUID userId = UUID.fromString("018f5f8d-7a63-7000-8000-abcdef123456");

        String json = """
                {
                  "payload": {
                    "attendanceId": "018f5f8d-7a63-7000-8000-123456789abc",
                    "attendanceDate": "2026-05-05T09:00:00",
                    "checkTime": "2026-05-05T09:03:00",
                    "status": "LATE",
                    "memo": "subway delay",
                    "userId": "018f5f8d-7a63-7000-8000-abcdef123456"
                  }
                }
                """;

        AttendanceCheckEvent event = AttendanceCheckEvent.fromJson(json);

        assertThat(event.getAttendanceId()).isEqualTo(attendanceId);
        assertThat(event.getAttendanceDate()).isEqualTo(LocalDateTime.parse("2026-05-05T09:00:00"));
        assertThat(event.getCheckTime()).isEqualTo(LocalDateTime.parse("2026-05-05T09:03:00"));
        assertThat(event.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(event.getMemo()).isEqualTo("subway delay");
        assertThat(event.getUserId()).isEqualTo(userId);
    }

    @Test
    void fromJsonReadsTextPayload() {
        String json = """
                {
                  "payload": "{\\"attendanceId\\":\\"018f5f8d-7a63-7000-8000-123456789abc\\",\\"attendanceDate\\":\\"2026-05-05T09:00:00\\",\\"checkTime\\":\\"2026-05-05T09:03:00\\",\\"status\\":\\"PRESENT\\",\\"memo\\":\\"ok\\",\\"userId\\":\\"018f5f8d-7a63-7000-8000-abcdef123456\\"}"
                }
                """;

        AttendanceCheckEvent event = AttendanceCheckEvent.fromJson(json);

        assertThat(event.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(event.getMemo()).isEqualTo("ok");
    }
}
