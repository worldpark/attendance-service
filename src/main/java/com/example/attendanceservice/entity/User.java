package com.example.attendanceservice.entity;

import com.example.attendanceservice.enumeratedType.Role;
import com.example.attendanceservice.util.UUIDv6Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "users")
public class User extends BaseEntity{

    @Id
    private UUID userId;

    @NotNull
    @Column(unique = true)
    private String username;

//    @NotNull
    private String password;
    @NotNull
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<Attendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "assignedUser", cascade = CascadeType.REMOVE)
    private List<AttendanceTarget> assignTargets = new ArrayList<>();

    public User(UUID userId, String username, String password, String name, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public User(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    public void prePersist() {
        if (userId == null) {
            userId = UUIDv6Generator.generate();
        }
    }

    public void changeInfo(String password, String name, Role role){
        this.password = password;
        this.name = name;
        this.role = role;
    }

}
