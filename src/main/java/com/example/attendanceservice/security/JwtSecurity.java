package com.example.attendanceservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class JwtSecurity {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    public boolean checkingJwt(String jwt){

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        jwt = jwt.replace("Bearer ", "");

        try{


            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return true;
        }catch (Exception exception){
            log.error("JWT 검증 실패", exception);

            return false;
        }
    }
}
