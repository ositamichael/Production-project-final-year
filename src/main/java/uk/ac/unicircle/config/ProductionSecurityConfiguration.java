package uk.ac.unicircle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@Profile("prod")
public class ProductionSecurityConfiguration {

    @Bean
    SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/favicon.svg", "/manifest.webmanifest", "/service-worker.js",
                                "/offline.html", "/privacy.html", "/acceptable-use.html", "/accessibility.html",
                                "/robots.txt", "/css/**", "/js/**", "/images/**",
                                "/error", "/sign-in", "/api/platform/status", "/api/security/csrf", "/actuator/health",
                                "/api/assistant/**", "/oauth2/**", "/login/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        RegexRequestMatcher.regexMatcher("^/api/.*")))
                .oauth2Login(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(SecurityHeaders.CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                SecurityHeaders.PERMISSIONS_POLICY)))
                .logout(logout -> logout.logoutSuccessUrl("/?signed-out=1#campus"))
                .build();
    }
}
