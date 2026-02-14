package com.ndl.numbers_dont_lie.assistant.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AssistantPiiFilterService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    public String sanitizeOutput(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String sanitized = EMAIL_PATTERN.matcher(response).replaceAll("[redacted-email]");
        sanitized = sanitized.replaceAll("(?i)password\\s*[:=]\\s*\\S+", "password: [redacted]");
        sanitized = sanitized.replaceAll("(?i)token\\s*[:=]\\s*\\S+", "token: [redacted]");
        return sanitized;
    }
}
