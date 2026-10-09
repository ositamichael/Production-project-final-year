ALTER TABLE connections ADD COLUMN pair_key VARCHAR(73);

UPDATE connections
SET pair_key = CASE
    WHEN CAST(requester_id AS VARCHAR) < CAST(addressee_id AS VARCHAR)
        THEN CAST(requester_id AS VARCHAR) || ':' || CAST(addressee_id AS VARCHAR)
    ELSE CAST(addressee_id AS VARCHAR) || ':' || CAST(requester_id AS VARCHAR)
END;

ALTER TABLE connections ALTER COLUMN pair_key SET NOT NULL;
ALTER TABLE connections ADD CONSTRAINT uq_connection_unordered_pair
    UNIQUE (university_id, pair_key);

ALTER TABLE conversations ADD COLUMN direct_pair_key VARCHAR(73);
ALTER TABLE conversations ADD CONSTRAINT uq_direct_conversation_pair
    UNIQUE (university_id, direct_pair_key);
ALTER TABLE conversations ADD CONSTRAINT ck_direct_conversation_pair
    CHECK (
        (conversation_type = 'DIRECT' AND direct_pair_key IS NOT NULL)
        OR (conversation_type = 'GROUP' AND direct_pair_key IS NULL)
    );

CREATE INDEX idx_connections_participants
    ON connections(university_id, requester_id, addressee_id, status);
CREATE INDEX idx_conversation_members_user
    ON conversation_members(user_id, conversation_id);
