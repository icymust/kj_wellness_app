package com.ndl.numbers_dont_lie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

  @Bean
  @Order(-1)
  SecurityFilterChain shoppingListPublicChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher(new AntPathRequestMatcher("/api/shopping-list/**"))
      .csrf(csrf -> csrf.disable())
      .cors(c -> {})
      .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Order(0)
  SecurityFilterChain apiPublicChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher(new AntPathRequestMatcher("/api/**"))
      .csrf(csrf -> csrf.disable())
      .cors(c -> {})
      .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler success, JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(c -> {})
      .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
    .authorizeHttpRequests(auth -> auth
      // allow preflight CORS requests without authentication
      .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        // shopping list endpoints (no authentication required)
        .requestMatchers(new AntPathRequestMatcher("/api/shopping-list/**")).permitAll()
        // explicit daily meal-plan endpoints (workaround for 401 on /day)
        .requestMatchers(HttpMethod.GET, "/api/meal-plans/day").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/meal-plans/day/**").permitAll()
        // debug endpoints (no authentication required) - must be before auth/** to override OAuth2
        .requestMatchers("/api/debug/**").permitAll()
        // recipe read endpoints (no authentication required)
        .requestMatchers("/api/recipes/**").permitAll()
      // shopping list endpoints (no authentication required)
      .requestMatchers("/api/shopping-list/**").permitAll()
      // allow meal move/reorder without auth (dev)
      .requestMatchers(HttpMethod.POST, "/api/meal-plans/meals/**").permitAll()
      .requestMatchers(HttpMethod.POST, "/api/meal-plans/day/meals/**").permitAll()
      // TEMPORARY: production meal plan endpoints (no authentication required)
      // TODO: Remove when authentication is implemented
      .requestMatchers("/api/meal-plans/**").permitAll()
      // TEMPORARY: allow all /api/** during local development
      .requestMatchers("/api/**").permitAll()
      // TEMPORARY: allow profile read/update without auth for end-to-end testing
      .requestMatchers(HttpMethod.GET, "/profile/**").permitAll()
      .requestMatchers(HttpMethod.PUT, "/profile/**").permitAll()
      .requestMatchers("/auth/**","/health","/login/**","/oauth2/**","/oauth-callback/**").permitAll()
      // dev/test helpers (non-sensitive): allow temporarily for local verification
      .requestMatchers("/dev/**").permitAll()
      .anyRequest().permitAll()
    )
      .oauth2Login(o -> o
          .successHandler(success)
      );
    // Ensure JWT filter runs before rate limiter so AUTH_EMAIL is available (per-user rate limiting)
    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        .requestMatchers("/api/meal-plans/meals/**")
        .requestMatchers("/api/shopping-list/**");
  }
}
