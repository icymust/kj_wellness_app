package com.ndl.numbers_dont_lie.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final int accessTtlMin;
    private final int refreshTtlDays;

    public JwtService(org.springframework.core.env.Environment env) {
        String secret = env.getProperty("app.jwt.secret");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret is missing or too short");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMin = Integer.parseInt(env.getProperty("app.jwt.access-ttl-min", "15"));
        this.refreshTtlDays = Integer.parseInt(env.getProperty("app.jwt.refresh-ttl-days", "7"));
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlMin * 60L);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "typ", "access"   // тип токена
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlDays * 24L * 3600L);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "typ", "refresh"  // тип токена
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt);
    }

    public String getEmail(String jwt) {
        return parse(jwt).getBody().getSubject();
    }

    public boolean isAccessToken(String jwt) {
        Object typ = parse(jwt).getBody().get("typ");
        return "access".equals(typ);
    }

    public boolean isRefreshToken(String jwt) {
        Object typ = parse(jwt).getBody().get("typ");
        return "refresh".equals(typ);
    }
}
