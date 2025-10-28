package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.dto.ConsentDto;
import com.ndl.numbers_dont_lie.entity.UserConsent;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserConsentRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PrivacyService {
  private final UserRepository users;
  private final UserConsentRepository consents;

  public PrivacyService(UserRepository users, UserConsentRepository consents) {
    this.users = users; this.consents = consents;
  }

  private UserEntity mustUser(String email) {
    return users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
  }

  public UserConsent getOrCreate(String email) {
    var u = mustUser(email);
    return consents.findByUser(u).orElseGet(() -> {
      var c = new UserConsent();
      c.setUser(u);
      c.setAccepted(false);
      c.setVersion("1.0");
      c.setAcceptedAt(Instant.EPOCH);
      c.setAllowAiUseProfile(false);
      c.setAllowAiUseHistory(false);
      c.setAllowAiUseHabits(false);
      c.setPublicProfile(false);
      c.setPublicStats(false);
      c.setEmailProduct(false);
      c.setEmailSummaries(false);
      return consents.save(c);
    });
  }

  public UserConsent update(String email, ConsentDto dto) {
    var c = getOrCreate(email);
    c.setAccepted(dto.accepted);
    c.setVersion(dto.version != null ? dto.version : c.getVersion());
    c.setAcceptedAt(dto.accepted ? Instant.now() : c.getAcceptedAt());
    c.setAllowAiUseProfile(dto.allowAiUseProfile);
    c.setAllowAiUseHistory(dto.allowAiUseHistory);
    c.setAllowAiUseHabits(dto.allowAiUseHabits);
    c.setPublicProfile(dto.publicProfile);
    c.setPublicStats(dto.publicStats);
    c.setEmailProduct(dto.emailProduct);
    c.setEmailSummaries(dto.emailSummaries);
    return consents.save(c);
  }
}
