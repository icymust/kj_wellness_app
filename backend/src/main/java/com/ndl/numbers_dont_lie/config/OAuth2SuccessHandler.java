package com.ndl.numbers_dont_lie.config;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository users;
  private final JwtService jwt;
  private final String frontendOrigin;

  public OAuth2SuccessHandler(UserRepository users, JwtService jwt,
                              @org.springframework.beans.factory.annotation.Value("${FRONTEND_ORIGIN:http://localhost:5173}") String frontendOrigin) {
    this.users = users;
    this.jwt = jwt;
    this.frontendOrigin = frontendOrigin;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {

    OAuth2User oUser = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attrs = oUser.getAttributes();

    String email = (String) attrs.getOrDefault("email", null);
    if (email == null && attrs.get("login") != null) {
      email = attrs.get("login") + "@users.noreply.github.com";
    }

    if (email == null) {
      response.sendRedirect(frontendOrigin + "/oauth-callback#error=missing_email");
      return;
    }

    final String em = email;
    UserEntity user = users.findByEmail(em).orElseGet(() -> {
      UserEntity u = new UserEntity();
      u.setEmail(em);
      u.setPasswordHash("OAUTH");
      return u;
    });
    user.setEmailVerified(true);
    users.save(user);

    // Enforce 2FA challenge if enabled
    if (user.isTwoFactorEnabled()) {
      String temp = jwt.generatePre2faToken(user);
      String redirect = frontendOrigin + "/oauth-callback#need2fa=1&tempToken=" + java.net.URLEncoder.encode(temp, java.nio.charset.StandardCharsets.UTF_8);
      response.sendRedirect(redirect);
      return;
    }

    // Otherwise issue tokens immediately
    String access = jwt.generateAccessToken(user);
    String refresh = jwt.generateRefreshToken(user);
    String redirect = frontendOrigin + "/oauth-callback#accessToken=" + access + "&refreshToken=" + refresh;
    response.sendRedirect(redirect);
  }
}
