package uk.ac.unicircle.social;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side gate for every browser call signal. A browser cannot call an
 * arbitrary account merely by knowing its identifier.
 */
@Service
class CallPermissionService {

    private final JdbcTemplate jdbc;

    CallPermissionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    CallPeer acceptedPeer(UUID senderId, UUID targetId) {
        return jdbc.query("""
                SELECT sender.display_name
                FROM user_accounts sender
                JOIN user_accounts target ON target.id = ?
                JOIN connections connection
                  ON connection.university_id = sender.university_id
                 AND connection.pair_key = ?
                 AND connection.status = 'ACCEPTED'
                WHERE sender.id = ?
                  AND sender.university_id = target.university_id
                """, (result, row) -> new CallPeer(senderId, result.getString(1)),
                targetId, SocialService.pairKey(senderId, targetId), senderId)
                .stream().findFirst().orElse(null);
    }

    record CallPeer(UUID accountId, String displayName) {
    }
}
