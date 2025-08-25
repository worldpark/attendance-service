package com.example.attendanceservice.enumeratedType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Role {
    ROLE_DEV(CONST.DEV)
    , ROLE_ADMIN(CONST.ADMIN)
    , ROLE_USER(CONST.USER);

    private final String roleName;

    public static class CONST {
        public static final String DEV = "ROLE_DEV";
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String USER = "ROLE_USER";
    }
}
