package org.soprasteria.avans.lockercloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Open toegang tot Swagger-UI en API-docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Open toegang tot Thymeleaf pagina en andere statische resources
                        .requestMatchers("/", "/index", "/css/**", "/js/**", "/images/**", "/showCloudDirectory").permitAll()
                        // Overige endpoints (zoals /api/files/**) kunnen ook openbaar worden gemaakt als dat gewenst is
                        .requestMatchers("/api/files/**").permitAll()
                        // Alle andere requests vereisen authenticatie
//                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

}

