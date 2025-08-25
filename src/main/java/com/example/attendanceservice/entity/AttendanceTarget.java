package com.example.attendanceservice.entity;

import com.example.attendanceservice.util.UUIDv6Generator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "attendanceTargetUnique",
                        columnNames = {"user_id"}
                )
        }
)
public class AttendanceTarget extends BaseEntity{

    @Id
    private UUID targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        if (targetId == null) {
            targetId = UUIDv6Generator.generate();
        }
    }
}
