package com.ndl.numbers_dont_lie.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds AI availability flag with runtime override capability.
 */
@Service
public class AiAvailabilityService {
  private final AtomicBoolean enabled;

  public AiAvailabilityService(@Value("${app.ai.enabled:true}") boolean initialEnabled) {
    this.enabled = new AtomicBoolean(initialEnabled);
  }

  public boolean isEnabled() { return enabled.get(); }
  public void setEnabled(boolean value) { enabled.set(value); }
}
