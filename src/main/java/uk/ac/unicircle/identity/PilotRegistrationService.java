package uk.ac.unicircle.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("pilot")
class PilotRegistrationService {

    private static final UUID PILOT_UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345111");
    private static final UUID PILOT_PROGRAMME_ID = UUID.fromString("3d063eea-6387-4f55-a031-4af09f451101");

    private final UniversityRepository universities;
    private final UserAccountRepository accounts;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String inviteCode;

    PilotRegistrationService(UniversityRepository universities, UserAccountRepository accounts,
            JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
            @Value("${unicircle.pilot.invite-code:}") String inviteCode) {
        this.universities = universities;
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.inviteCode = inviteCode;
    }

    @PostConstruct
    void validateConfiguration() {
        if (inviteCode.length() < 32) {
            throw new IllegalStateException("PILOT_INVITE_CODE must contain at least 32 characters.");
        }
    }

    @Transactional
    void register(PilotRegistrationController.RegistrationRequest request) {
        if (!MessageDigest.isEqual(inviteCode.getBytes(StandardCharsets.UTF_8),
                request.inviteCode().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The pilot invitation is invalid.");
        }
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email.");
        }
        University pilot = universities.findById(PILOT_UNIVERSITY_ID)
                .orElseGet(() -> universities.saveAndFlush(new University(PILOT_UNIVERSITY_ID,
                        "pilot-campus", "UniCircle Campus Pilot", "pilot.unicircle.invalid", null)));
        UserAccount account = accounts.saveAndFlush(new UserAccount(UUID.randomUUID(), pilot,
                "pilot-" + UUID.randomUUID(), email, request.displayName().strip(), UserRole.STUDENT));
        account.updateProfile(request.displayName(), null, request.courseName(), request.yearOfStudy(),
                ProfileVisibility.UNIVERSITY_ONLY);
        accounts.saveAndFlush(account);
        jdbc.update("INSERT INTO pilot_credentials (account_id, password_hash) VALUES (?, ?)",
                account.id(), passwordEncoder.encode(request.password()));
        jdbc.update("""
                INSERT INTO enrolments
                  (id, university_id, user_id, programme_id, year_of_study, status, started_on)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_DATE)
                """, UUID.randomUUID(), PILOT_UNIVERSITY_ID, account.id(), PILOT_PROGRAMME_ID,
                request.yearOfStudy());
    }
}
