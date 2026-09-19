package com.alok.home.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OncePerRequestFilter customAuthenticationFilter) throws Exception {

        return http
                // ... your existing cors, csrf, and authorizeHttpRequests configuration ...
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable()) // Completely disables the X-Frame-Options header
                )
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator", "/actuator/**").permitAll();
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/v3/api-docs.yaml",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/swagger-ui/index.html",
                            "/webjars/**"
                    ).permitAll();
                    auth.requestMatchers(
                                    "/notes/tree",
                                    "/notes/content",
                                    "/notes/images/**",
                                    "/notes/**",
                                    "/docs/pdfs/tree",
                                    "/docs/pdfs/stream"
                            )
                            .hasAnyRole("ADMIN", "USER", "home_api_ro", "home_api_rw", "LOCALHOST");

                    auth.anyRequest().authenticated();
                })
                //.httpBasic(Customizer.withDefaults())
                .build();
    }
}
