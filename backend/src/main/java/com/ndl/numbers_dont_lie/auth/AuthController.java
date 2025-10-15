package com.ndl.numbers_dont_lie.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.jsonwebtoken.JwtException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthStore store;        // хранит только verificationTokens
    private final AuthService authService; // работа с БД (users)
    private final JwtService jwt;

    

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("message", "Auth route works!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        try {
            UserEntity user = authService.register(email, password);

            String token = UUID.randomUUID().toString();
            store.getVerificationTokens().put(token, email);

            String verifyUrl = "http://localhost:5173/auth/verify?token=" + token;
            System.out.println("[MAIL-EMULATOR] Send verify link to " + email + ": " + verifyUrl);

            return ResponseEntity.status(201).body(Map.of(
                "message", "User registered successfully. Check console for verification link.",
                "user", Map.of("email", user.getEmail()),
                "verificationLink", verifyUrl
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        String email = store.getVerificationTokens().get(token);
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired token"));
        }
        try {
            UserEntity user = authService.verifyEmail(email);
            store.getVerificationTokens().remove(token);
            return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully",
                "user", Map.of("email", user.getEmail(), "emailVerified", user.isEmailVerified())
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public AuthController(AuthStore store, AuthService authService, JwtService jwt) {
    this.store = store;
    this.authService = authService;
    this.jwt = jwt;
}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.email == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }
        try {
            UserEntity user = authService.login(req.email, req.password);
            if (!user.isEmailVerified()) {
                return ResponseEntity.status(403).body(Map.of("error", "Email not verified"));
            }
            String access = jwt.generateAccessToken(user);
            String refresh = jwt.generateRefreshToken(user);
            return ResponseEntity.ok(new TokensResponse(access, refresh));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        if (req.refreshToken == null || req.refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing refreshToken"));
        }
        try {
            if (!jwt.isRefreshToken(req.refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token type"));
            }
            String email = jwt.getEmail(req.refreshToken);
            UserEntity user = authService.verifyEmail(email); // просто получим из БД; verifyEmail помечает true – это ок для уже верифицированных
            // Лучше сделать отдельный метод findByEmail без изменения состояния:
            // UserEntity user = userRepository.findByEmail(email).orElseThrow(...);
            String newAccess = jwt.generateAccessToken(user);
            return ResponseEntity.ok(Map.of("accessToken", newAccess));
        } catch (JwtException | IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
    }

}
