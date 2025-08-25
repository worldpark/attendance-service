package com.example.attendanceservice.util;

import java.util.UUID;

public class UUIDv6Generator {

    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        long mostSigBits = (timestamp << 32) | ((timestamp & 0xFFFF0000) >> 16) | 0x6000;
        long leastSigBits = UUID.randomUUID().getLeastSignificantBits();
        return new UUID(mostSigBits, leastSigBits);
    }
}
