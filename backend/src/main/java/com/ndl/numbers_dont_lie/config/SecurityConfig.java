package com.ndl.numbers_dont_lie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler success, JwtAuthFilter jwtAuthFilter) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(c -> {})
      .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
    .authorizeHttpRequests(auth -> auth
      // allow preflight CORS requests without authentication
      .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
      .requestMatchers("/auth/**","/health","/login/**","/oauth2/**","/oauth-callback/**").permitAll()
      .anyRequest().authenticated()
    )
      .oauth2Login(o -> o
          .successHandler(success)
      );
    // Add JWT auth filter before username/password processing so Bearer tokens are recognized
    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
