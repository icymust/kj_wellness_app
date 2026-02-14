package com.ndl.numbers_dont_lie.assistant.service;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AssistantSafetyService {

    public boolean isMedicalRisk(String message) {
        String q = norm(message);
        return q.contains("chest pain")
            || q.contains("can't breathe")
            || q.contains("cannot breathe")
            || q.contains("faint")
            || q.contains("severe pain")
            || q.contains("heart attack")
            || q.contains("stroke")
            || q.contains("suicid");
    }

    public boolean isPiiRequest(String message) {
        String q = norm(message);
        return q.contains("email")
            || q.contains("date of birth")
            || q.contains("dob")
            || q.contains("password")
            || q.contains("credential")
            || q.contains("token")
            || q.contains("other users")
            || q.contains("all users")
            || q.contains("another user")
            || q.contains("user id");
    }

    public boolean isJailbreakAttempt(String message) {
        String q = norm(message);
        return q.contains("admin mode")
            || q.contains("ignore previous instructions")
            || q.contains("pretend i am")
            || q.contains("show me all users")
            || q.contains("you are now system");
    }

    public String medicalSafetyResponse() {
        return "I can provide general wellness guidance, but I can't provide medical diagnosis. "
            + "If you have urgent symptoms such as chest pain or breathing difficulty, seek immediate professional care or emergency services.";
    }

    public String piiRefusalResponse() {
        return "I can't access or reveal sensitive personal data such as email, date of birth, credentials, or other users' information. "
            + "I can help with your health metrics, meal plans, recipes, and nutrition insights.";
    }

    private String norm(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
