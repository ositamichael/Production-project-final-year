package uk.ac.unicircle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@Profile("pilot")
public class PilotSecurityConfiguration {

    @Bean
    SecurityFilterChain pilotSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/pilot-login.html", "/favicon.svg", "/css/**", "/js/**", "/images/**",
                                "/error", "/sign-in", "/api/pilot/register", "/api/platform/status", "/api/security/csrf",
                                "/api/assistant/**", "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        RegexRequestMatcher.regexMatcher("^/api/.*")))
                .formLogin(login -> login
                        .loginPage("/pilot-login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/?signed-in=1#campus", true)
                        .failureUrl("/pilot-login.html?error=1")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?signed-out=1#campus"))
                .build();
    }

    @Bean
    PasswordEncoder pilotPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService pilotUsers(JdbcTemplate jdbc) {
        return username -> jdbc.query("""
                SELECT ua.email, pc.password_hash
                FROM pilot_credentials pc
                JOIN user_accounts ua ON ua.id = pc.account_id
                JOIN universities u ON u.id = ua.university_id
                WHERE LOWER(ua.email) = LOWER(?) AND u.slug = 'pilot-campus' AND u.active = TRUE
                """, (result, row) -> User.withUsername(result.getString(1))
                        .password(result.getString(2)).roles("STUDENT").build(), username)
                .stream().findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Pilot account not found"));
    }
}
