
package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private final UserRepository users;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
        UserEntity user = users.findByEmail(email).orElse(null);
        if (user == null) {
            log.debug("Login failed: user not found for email={}", email);
            throw new IllegalStateException("user_not_found");
        }
        boolean ok = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        if (!ok) {
            log.debug("Login failed: bad password for email={}", email);
            throw new IllegalStateException("bad_password");
        }
        if (user.isTwoFactorEnabled()) {
            log.debug("Login accepted (needs 2FA) for email={}", email);
        } else {
            log.debug("Login accepted (no 2FA) for email={}", email);
        }
        return user;
    }

}
