package com.example.attendanceservice.service;

import com.example.attendanceservice.aop.DistributedLock;
import com.example.attendanceservice.dto.AttendanceCheckEvent;
import com.example.attendanceservice.entity.Attendance;
import com.example.attendanceservice.entity.User;
import com.example.attendanceservice.enumeratedType.AttendanceStatus;
import com.example.attendanceservice.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Transactional
    @DistributedLock(
            key = "#attendanceCheckEvent.userId + ':' + #attendanceCheckEvent.attendanceDate",
            waitTime = 5L,
            leaseTime = 10L
    )
    public void saveAttendance(AttendanceCheckEvent attendanceCheckEvent) {
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

        if (attendanceCheckEvent.getCheckTime().isBefore(attendance.getAttendanceDate())) {
            attendance.changeStatus(AttendanceStatus.PRESENT);
        } else {
            attendance.changeStatus(AttendanceStatus.LATE);
        }

        attendanceRepository.save(attendance);
    }
}
