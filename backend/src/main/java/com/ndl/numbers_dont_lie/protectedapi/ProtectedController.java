package com.ndl.numbers_dont_lie.protectedapi;

import com.ndl.numbers_dont_lie.auth.UserEntity;
import com.ndl.numbers_dont_lie.auth.UserRepository;
import com.ndl.numbers_dont_lie.auth.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/protected")
public class ProtectedController {

    private final UserRepository users;
    private final JwtService jwtService;

    public ProtectedController(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = authHeader.substring("Bearer ".length());

        try {
            if (!jwtService.isAccessToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token type"));
            }
            String email = jwtService.getEmail(token);
            UserEntity user = users.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }
            if (!user.isEmailVerified()) {
                return ResponseEntity.status(403).body(Map.of("error", "Email not verified"));
            }
            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "emailVerified", true
            ));
        } catch (io.jsonwebtoken.JwtException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
    }   
}
