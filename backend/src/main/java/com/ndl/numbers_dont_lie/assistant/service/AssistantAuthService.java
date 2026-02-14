package com.ndl.numbers_dont_lie.assistant.service;

import com.ndl.numbers_dont_lie.auth.service.JwtService;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AssistantAuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AssistantAuthService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public UserEntity requireUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        String token = authorizationHeader.substring("Bearer ".length());
        try {
            if (!jwtService.isAccessToken(token)) {
                throw new IllegalStateException("Invalid token type");
            }
            String email = jwtService.getEmail(token);
            return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        } catch (JwtException e) {
            throw new IllegalStateException("Invalid or expired token");
        }
    }
}
