package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.store.AuthStore;
import com.ndl.numbers_dont_lie.service.AuthService;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.dto.LoginRequest;
import com.ndl.numbers_dont_lie.dto.RefreshRequest;
import com.ndl.numbers_dont_lie.dto.TokensResponse;
import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.TwoFactorService;
import com.ndl.numbers_dont_lie.service.PasswordResetService;
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
    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    

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

    public AuthController(AuthStore store, AuthService authService, JwtService jwt, TwoFactorService twoFactorService, UserRepository userRepository, PasswordResetService passwordResetService) {
        this.store = store;
        this.authService = authService;
        this.jwt = jwt;
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
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
            if (user.isTwoFactorEnabled()) {
                String temp = jwt.generatePre2faToken(user);
                return ResponseEntity.ok(Map.of("need2fa", true, "tempToken", temp));
            }
            String access = jwt.generateAccessToken(user);
            String refresh = jwt.generateRefreshToken(user);
            return ResponseEntity.ok(new TokensResponse(access, refresh));
        } catch (IllegalStateException e) {
            // pass through specific error codes for debugging (user_not_found / bad_password)
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // password reset endpoints живут в PasswordResetController

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

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String tempToken = body.get("tempToken");
        if (code == null || tempToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }
        try {
            if (!jwt.isPre2faToken(tempToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token type"));
            }
            String email = jwt.getEmail(tempToken);
            UserEntity user = authService.verifyEmail(email); // просто извлечь пользователя
            if (!user.isTwoFactorEnabled()) return ResponseEntity.status(400).body(Map.of("error", "2FA not enabled"));

            boolean ok;
            if (code.matches("\\d{6}")) {
                ok = twoFactorService.verifyForLogin(email, code);
            } else {
                ok = twoFactorService.useRecoveryForLogin(email, code);
            }
            if (!ok) return ResponseEntity.status(401).body(Map.of("error", "Invalid code"));

            String access = jwt.generateAccessToken(user);
            String refresh = jwt.generateRefreshToken(user);
            return ResponseEntity.ok(new TokensResponse(access, refresh));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
    }

    // TEMP DEBUG endpoint — expose minimal user state to diagnose 2FA login issues.
    // DO NOT enable in production.
    @GetMapping("/debug/user")
    public ResponseEntity<?> debugUser(@RequestParam String email) {
        return userRepository.findByEmail(email)
            .map(u -> ResponseEntity.ok(Map.of(
                "email", u.getEmail(),
                "emailVerified", u.isEmailVerified(),
                "twoFactorEnabled", u.isTwoFactorEnabled(),
                "hasSecret", u.getTotpSecret() != null && !u.getTotpSecret().isBlank(),
                "recoveryCodesPresent", u.getRecoveryCodesJson() != null && !u.getRecoveryCodesJson().isBlank()
            )))
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not_found")));
    }

}
