CREATE TABLE assistant_conversations (
    id UUID PRIMARY KEY,
    owner_key VARCHAR(512) NOT NULL,
    title VARCHAR(120) NOT NULL,
    mode VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_assistant_conversation_mode CHECK (mode IN
      ('GENERAL', 'UNIVERSITY_RESEARCH', 'STUDY_COACH', 'CODE_LAB', 'IMAGE_STUDIO'))
);

CREATE INDEX idx_assistant_conversations_owner_updated
    ON assistant_conversations(owner_key, updated_at DESC);

CREATE TABLE assistant_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES assistant_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    mode VARCHAR(40) NOT NULL,
    image_data_url TEXT,
    notice TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_assistant_message_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_assistant_message_mode CHECK (mode IN
      ('GENERAL', 'UNIVERSITY_RESEARCH', 'STUDY_COACH', 'CODE_LAB', 'IMAGE_STUDIO')),
    CONSTRAINT ck_assistant_message_content CHECK (CHAR_LENGTH(TRIM(content)) > 0)
);

CREATE INDEX idx_assistant_messages_conversation_time
    ON assistant_messages(conversation_id, created_at, id);

CREATE TABLE assistant_message_sources (
    message_id UUID NOT NULL REFERENCES assistant_messages(id) ON DELETE CASCADE,
    source_position INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    PRIMARY KEY (message_id, source_position)
);
