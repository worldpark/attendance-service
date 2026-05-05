package com.example.attendanceservice.repository;

import com.example.attendanceservice.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByUser_UserIdAndAttendanceDate(UUID userId, LocalDateTime attendanceDate);
}
