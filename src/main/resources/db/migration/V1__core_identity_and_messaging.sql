CREATE TABLE universities (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(180) NOT NULL,
    email_domain VARCHAR(180) NOT NULL UNIQUE,
    oidc_issuer VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    external_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    role VARCHAR(30) NOT NULL,
    profile_visibility VARCHAR(30) NOT NULL DEFAULT 'UNIVERSITY_ONLY',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_identity UNIQUE (university_id, external_subject),
    CONSTRAINT uq_user_email_per_university UNIQUE (university_id, email),
    CONSTRAINT ck_user_role CHECK (role IN ('STUDENT', 'STAFF', 'SERVICE_PROVIDER', 'ADMIN')),
    CONSTRAINT ck_profile_visibility CHECK (profile_visibility IN ('PRIVATE', 'CONNECTIONS', 'UNIVERSITY_ONLY'))
);

CREATE INDEX idx_user_accounts_university ON user_accounts(university_id);

CREATE TABLE connections (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    requester_id UUID NOT NULL REFERENCES user_accounts(id),
    addressee_id UUID NOT NULL REFERENCES user_accounts(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_connection_pair UNIQUE (university_id, requester_id, addressee_id),
    CONSTRAINT ck_connection_distinct_users CHECK (requester_id <> addressee_id),
    CONSTRAINT ck_connection_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'BLOCKED'))
);

CREATE INDEX idx_connections_university ON connections(university_id);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    conversation_type VARCHAR(20) NOT NULL,
    title VARCHAR(160),
    created_by UUID NOT NULL REFERENCES user_accounts(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_conversation_type CHECK (conversation_type IN ('DIRECT', 'GROUP'))
);

CREATE INDEX idx_conversations_university ON conversations(university_id);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES user_accounts(id),
    body VARCHAR(4000) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_message_body_not_blank CHECK (CHAR_LENGTH(TRIM(body)) > 0)
);

CREATE INDEX idx_messages_conversation_time ON messages(conversation_id, sent_at);
CREATE INDEX idx_messages_university ON messages(university_id);
