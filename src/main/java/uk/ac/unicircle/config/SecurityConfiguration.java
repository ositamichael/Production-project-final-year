package uk.ac.unicircle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!prod")
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/favicon.svg", "/css/**", "/js/**",
                                "/api/platform/status", "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
