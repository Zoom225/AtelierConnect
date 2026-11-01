package com.atelierconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/sites/**", "/api/centres/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/terrains/**", "/api/salles/**").permitAll()
                        .requestMatchers("/api/sites/**", "/api/centres/**", "/api/terrains/**", "/api/salles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/membres/login", "/api/participants/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/matches/public", "/api/ateliers/public").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/matches", "/api/ateliers").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/matches/organisateur/**", "/api/ateliers/organisateur/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/matches", "/api/ateliers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/matches/site/**", "/api/ateliers/site/**", "/api/matches/centre/**", "/api/ateliers/centre/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/matches/*", "/api/ateliers/*").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/matches/*", "/api/ateliers/*").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, "/api/matches/*/cancel", "/api/ateliers/*/cancel").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, "/api/matches/*/consent-open-public", "/api/ateliers/*/consent-open-public").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, "/api/matches/*/convert-public", "/api/ateliers/*/convert-public").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reservations/membre/**", "/api/reservations/participant/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/match/**", "/api/reservations/atelier/**").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/cancel").hasRole("MEMBER")

                        .requestMatchers("/api/paiements/**").hasRole("MEMBER")

                        .requestMatchers("/api/jours-fermeture/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
