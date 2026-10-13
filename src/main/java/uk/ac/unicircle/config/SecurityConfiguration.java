package uk.ac.unicircle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@Profile("!prod & !pilot")
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/demo-login.html", "/favicon.svg", "/manifest.webmanifest",
                                "/service-worker.js", "/offline.html", "/privacy.html", "/acceptable-use.html",
                                "/accessibility.html", "/robots.txt", "/css/**", "/js/**", "/images/**",
                                "/error", "/sign-in", "/api/platform/status", "/api/security/csrf", "/api/assistant/**",
                                "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        RegexRequestMatcher.regexMatcher("^/api/.*")))
                .formLogin(login -> login
                        .loginPage("/demo-login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/?signed-in=1#campus", true)
                        .failureUrl("/demo-login.html?error=1")
                        .permitAll())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(SecurityHeaders.CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                SecurityHeaders.PERMISSIONS_POLICY)))
                .logout(logout -> logout.logoutSuccessUrl("/?signed-out=1#campus"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService demoUsers(
            PasswordEncoder passwordEncoder,
            @Value("${unicircle.demo.username}") String username,
            @Value("${unicircle.demo.password}") String password) {
        var demoStudent = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("STUDENT")
                .build();
        var michael = User.withUsername("michael.carter@demo.university.example")
                .password(passwordEncoder.encode(password)).roles("STUDENT").build();
        var aisha = User.withUsername("aisha.khan@students.unicircle.test")
                .password(passwordEncoder.encode(password)).roles("STUDENT").build();
        var james = User.withUsername("james.wilson@students.unicircle.test")
                .password(passwordEncoder.encode(password)).roles("STUDENT").build();
        var sophie = User.withUsername("sophie.patel@students.unicircle.test")
                .password(passwordEncoder.encode(password)).roles("STUDENT").build();
        var reviewer = User.withUsername("reviewer@demo.university.example")
                .password(passwordEncoder.encode(password)).roles("STAFF").build();
        return new InMemoryUserDetailsManager(demoStudent, michael, aisha, james, sophie, reviewer);
    }
}
