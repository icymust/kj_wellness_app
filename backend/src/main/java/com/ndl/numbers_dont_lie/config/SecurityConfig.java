package com.ndl.numbers_dont_lie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler success) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(c -> {})
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/auth/**","/health","/login/**","/oauth2/**","/oauth-callback/**").permitAll()
          .anyRequest().authenticated()
      )
      .oauth2Login(o -> o
          .successHandler(success)
      );
    return http.build();
  }
}
