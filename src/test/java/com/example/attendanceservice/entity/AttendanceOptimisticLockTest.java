package com.example.attendanceservice.entity;

import com.example.attendanceservice.enumeratedType.AttendanceStatus;
import com.example.attendanceservice.enumeratedType.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class AttendanceOptimisticLockTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void throwsOptimisticLockExceptionWhenSameAttendanceIsUpdatedConcurrently() {
        UUID userId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        saveAttendance(userId, attendanceId);

        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            secondEntityManager.getTransaction().begin();

            Attendance firstAttendance = firstEntityManager.find(Attendance.class, attendanceId);
            Attendance secondAttendance = secondEntityManager.find(Attendance.class, attendanceId);

            firstAttendance.changeStatus(AttendanceStatus.PRESENT);
            firstEntityManager.getTransaction().commit();

            secondAttendance.changeCheckTime(LocalDateTime.parse("2026-05-05T09:10:00"));

            assertThatThrownBy(() -> secondEntityManager.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            rollbackIfActive(firstEntityManager);
            rollbackIfActive(secondEntityManager);
            firstEntityManager.close();
            secondEntityManager.close();
            deleteAttendanceAndUser(attendanceId, userId);
        }
    }

    private void saveAttendance(UUID userId, UUID attendanceId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            User user = User.builder()
                    .userId(userId)
                    .username("optimistic-" + userId)
                    .password("password")
                    .name("optimistic-lock-user")
                    .role(Role.ROLE_USER)
                    .build();

            Attendance attendance = Attendance.builder()
                    .attendanceId(attendanceId)
                    .attendanceDate(LocalDateTime.parse("2026-05-05T09:00:00"))
                    .checkTime(LocalDateTime.parse("2026-05-05T09:05:00"))
                    .status(AttendanceStatus.LATE)
                    .memo("created")
                    .user(user)
                    .build();

            entityManager.persist(user);
            entityManager.persist(attendance);
            entityManager.getTransaction().commit();
        } finally {
            rollbackIfActive(entityManager);
            entityManager.close();
        }
    }

    private void deleteAttendanceAndUser(UUID attendanceId, UUID userId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Attendance attendance = entityManager.find(Attendance.class, attendanceId);
            if (attendance != null) {
                entityManager.remove(attendance);
            }
            User user = entityManager.find(User.class, userId);
            if (user != null) {
                entityManager.remove(user);
            }
            entityManager.getTransaction().commit();
        } finally {
            rollbackIfActive(entityManager);
            entityManager.close();
        }
    }

    private void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }
}
