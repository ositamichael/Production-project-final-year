package uk.ac.unicircle.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("pilot")
class PilotPasswordRecoveryService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    PilotPasswordRecoveryService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    boolean resetPassword(String email, String recoveryCode, String newPassword) {
        String normalEmail = email.strip().toLowerCase(Locale.ROOT);
        String tokenHash = sha256(recoveryCode.strip());
        List<ResetToken> matches = jdbc.query("""
                SELECT reset.id, reset.account_id
                FROM pilot_password_reset_tokens reset
                JOIN user_accounts account ON account.id = reset.account_id
                JOIN universities university ON university.id = account.university_id
                WHERE LOWER(account.email) = LOWER(?)
                  AND reset.token_hash = ?
                  AND reset.used_at IS NULL
                  AND reset.expires_at > CURRENT_TIMESTAMP
                  AND account.account_status = 'ACTIVE'
                  AND university.slug = 'pilot-campus'
                FOR UPDATE
                """, (result, row) -> new ResetToken(
                        result.getObject("id", UUID.class),
                        result.getObject("account_id", UUID.class)), normalEmail, tokenHash);
        if (matches.size() != 1) return false;

        ResetToken token = matches.getFirst();
        int updated = jdbc.update("UPDATE pilot_credentials SET password_hash = ? WHERE account_id = ?",
                passwordEncoder.encode(newPassword), token.accountId());
        if (updated != 1) return false;
        jdbc.update("UPDATE pilot_password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?",
                token.id());
        return true;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private record ResetToken(UUID id, UUID accountId) {
    }
}
