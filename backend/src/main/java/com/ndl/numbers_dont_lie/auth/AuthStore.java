package com.ndl.numbers_dont_lie.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthStore {
   // email -> User
   private final Map<String, User> usersByEmail = new HashMap<>();
   // token -> email (верификация почты)
   private final Map<String, String> verificationTokens = new HashMap<>();

   public Map<String, User> getUsersByEmail() {
      return usersByEmail;
   }

   public Map<String, String> getVerificationTokens() {
      return verificationTokens;
   }
}
