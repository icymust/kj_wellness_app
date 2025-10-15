package com.ndl.numbers_dont_lie.auth;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public UserEntity register(String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new IllegalStateException("User already exists");
        }
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setPasswordHash(hash);
        return users.save(u);
    }

    public UserEntity verifyEmail(String email) {
        UserEntity user = users.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("User not found")
        );
        user.setEmailVerified(true);
        return users.save(user);
    }

    public UserEntity login(String email, String rawPassword) {
        UserEntity user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Invalid credentials"));
        if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
            throw new IllegalStateException("Invalid credentials");
        }
        return user;
    }   

}
