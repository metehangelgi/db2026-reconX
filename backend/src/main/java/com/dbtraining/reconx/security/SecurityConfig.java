package com.dbtraining.reconx.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ============================================================================
 * SecurityConfig — TICKET-ADV073 + TICKET-ADV074
 * ============================================================================
 * WHAT:    Spring Security filter chain. Production target: stateless JWT
 *          auth + method-level RBAC across ADMIN / TRADER / VIEWER /
 *          RECON_ANALYST roles.
 * HOW:     One SecurityFilterChain @Bean + PasswordEncoder @Bean +
 *          @EnableMethodSecurity. The JwtAuthenticationFilter is registered
 *          before UsernamePasswordAuthenticationFilter.
 * WHY:     Day 6 needs role-based protection on every endpoint, and the
 *          frontend uses bearer tokens issued at /auth/login.
 * OBSERVE: After Day-6 work is wired, GET /api/v1/trades without a token -> 401.
 * ============================================================================
 *
 *  DAY-1 DEFAULT (below): everything is `permitAll`. This lets the frontend
 *  and Swagger UI load on Day 1 without an auth UI. TICKET-ADV073 + ADV074
 *  replace this with proper JWT + role-based auth.
 *
 *  TODO(TICKET-ADV073 + ADV074):
 *    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 *
 *    @Bean
 *    public SecurityFilterChain filterChain(HttpSecurity http,
 *                                           JwtAuthenticationFilter jwtFilter) throws Exception {
 *        http
 *          .csrf(AbstractHttpConfigurer::disable)
 *          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 *          .authorizeHttpRequests(auth -> auth
 *            .requestMatchers("/auth/login","/actuator/health/**","/actuator/info",
 *                             "/actuator/prometheus","/swagger-ui.html","/swagger-ui/**",
 *                             "/v3/api-docs/**","/h2/**").permitAll()
 *            .requestMatchers(HttpMethod.GET,    "/v1/trades/**").hasAnyRole("VIEWER","TRADER","RECON_ANALYST","ADMIN")
 *            .requestMatchers(HttpMethod.POST,   "/v1/trades").hasAnyRole("TRADER","ADMIN")
 *            .requestMatchers(HttpMethod.PUT,    "/v1/trades/**").hasAnyRole("TRADER","ADMIN")
 *            .requestMatchers(HttpMethod.PATCH,  "/v1/trades/**").hasAnyRole("TRADER","ADMIN")
 *            .requestMatchers(HttpMethod.DELETE, "/v1/trades/**").hasRole("ADMIN")
 *            .requestMatchers("/v1/recon/**").hasAnyRole("RECON_ANALYST","ADMIN")
 *            .requestMatchers("/v1/audit/**").hasAnyRole("RECON_ANALYST","ADMIN")
 *            .anyRequest().authenticated())
 *          .headers(h -> h.frameOptions(f -> f.disable()))
 *          .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
 *        return http.build();
 *    }
 *
 *  HINT: Also add @EnableMethodSecurity on the class so @PreAuthorize on
 *        service methods is honoured.
 * ============================================================================
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        http
          .csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
          .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/login", "/actuator/health/**", "/actuator/info",
                             "/actuator/prometheus", "/swagger-ui.html", "/swagger-ui/**",
                             "/v3/api-docs/**", "/h2/**", "/v0/**").permitAll()
            .requestMatchers(HttpMethod.GET,    "/v1/trades/**").hasAnyRole("VIEWER", "TRADER", "RECON_ANALYST", "ADMIN")
            .requestMatchers(HttpMethod.POST,   "/v1/trades").hasAnyRole("TRADER", "ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/v1/trades/**").hasAnyRole("TRADER", "ADMIN")
            .requestMatchers(HttpMethod.PATCH,  "/v1/trades/**").hasAnyRole("TRADER", "ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/v1/trades/**").hasRole("ADMIN")
            .requestMatchers("/v1/recon/**").hasAnyRole("RECON_ANALYST", "ADMIN")
            .requestMatchers("/v1/audit/**").hasAnyRole("RECON_ANALYST", "ADMIN")
            .anyRequest().authenticated())
          .headers(h -> h.frameOptions(f -> f.disable()))
          .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JwtAuthenticationFilter is a @Component so it can be constructor-injected
     * above via addFilterBefore — but that same @Component annotation also
     * makes Spring Boot auto-register it a SECOND time as a generic servlet
     * filter applied to every URL, outside Spring Security's own chain. That
     * second, uncoordinated invocation runs at a different point in the filter
     * order than Security's addFilterBefore wiring; because
     * OncePerRequestFilter guards against re-entry per request, whichever
     * invocation runs first "wins" and the other is skipped — and if the
     * generic registration runs before Security's own context filter, the
     * SecurityContext it sets gets wiped before authorization checks run,
     * silently turning every valid JWT into an unauthenticated request (401).
     * Disabling the auto-registration keeps the bean usable for injection
     * while ensuring it only ever runs once, inside the chain built above.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
