package com.ndl.numbers_dont_lie.config;

import com.ndl.numbers_dont_lie.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) { this.jwtService = jwtService; }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring("Bearer ".length());
      try {
        if (jwtService.isAccessToken(token)) {
          String email = jwtService.getEmail(token);
          var authToken = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (JwtException | IllegalArgumentException e) {
        // invalid token — don't set authentication
      }
    }
    filterChain.doFilter(request, response);
  }
}
