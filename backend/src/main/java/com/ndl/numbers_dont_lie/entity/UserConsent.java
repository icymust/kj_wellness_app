package com.ndl.numbers_dont_lie.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_consents")
public class UserConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    @Column(nullable = false)
    private boolean accepted;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private Instant acceptedAt;

    @Column(nullable = false)
    private boolean allowAiUseProfile;

    @Column(nullable = false)
    private boolean allowAiUseHistory;

    @Column(nullable = false)
    private boolean allowAiUseHabits;

    @Column(nullable = false)
    private boolean publicProfile;

    @Column(nullable = false)
    private boolean publicStats;

    @Column(nullable = false)
    private boolean emailProduct;

    @Column(nullable = false)
    private boolean emailSummaries;

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }

    public boolean isAllowAiUseProfile() { return allowAiUseProfile; }
    public void setAllowAiUseProfile(boolean allowAiUseProfile) { this.allowAiUseProfile = allowAiUseProfile; }

    public boolean isAllowAiUseHistory() { return allowAiUseHistory; }
    public void setAllowAiUseHistory(boolean allowAiUseHistory) { this.allowAiUseHistory = allowAiUseHistory; }

    public boolean isAllowAiUseHabits() { return allowAiUseHabits; }
    public void setAllowAiUseHabits(boolean allowAiUseHabits) { this.allowAiUseHabits = allowAiUseHabits; }

    public boolean isPublicProfile() { return publicProfile; }
    public void setPublicProfile(boolean publicProfile) { this.publicProfile = publicProfile; }

    public boolean isPublicStats() { return publicStats; }
    public void setPublicStats(boolean publicStats) { this.publicStats = publicStats; }

    public boolean isEmailProduct() { return emailProduct; }
    public void setEmailProduct(boolean emailProduct) { this.emailProduct = emailProduct; }

    public boolean isEmailSummaries() { return emailSummaries; }
    public void setEmailSummaries(boolean emailSummaries) { this.emailSummaries = emailSummaries; }
}
