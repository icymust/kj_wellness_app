package com.ndl.numbers_dont_lie.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private String passwordHash; // хранится ХЕШ, не пароль

    // 2FA fields
    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "two_factor_verified_at")
    private java.time.Instant twoFactorVerifiedAt;

    // Store as JSON string for simplicity (ddl-auto=update). Use TEXT to avoid PostgreSQL Large Object (OID/CLOB) handling requirements.
    @Column(name = "recovery_codes_json", columnDefinition = "TEXT")
    private String recoveryCodesJson;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public java.time.Instant getTwoFactorVerifiedAt() { return twoFactorVerifiedAt; }
    public void setTwoFactorVerifiedAt(java.time.Instant twoFactorVerifiedAt) { this.twoFactorVerifiedAt = twoFactorVerifiedAt; }

    public String getRecoveryCodesJson() { return recoveryCodesJson; }
    public void setRecoveryCodesJson(String recoveryCodesJson) { this.recoveryCodesJson = recoveryCodesJson; }
}
