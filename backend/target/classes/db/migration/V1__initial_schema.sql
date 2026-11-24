-- Connectly Initial Schema Migration
-- Compatible with PostgreSQL 14+ and H2 (PostgreSQL compatibility mode)

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    status_message VARCHAR(255) DEFAULT 'Hey there! I am using Connectly.',
    is_online BOOLEAN DEFAULT FALSE,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    conversation_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_conversations_last_msg ON conversations (last_message_at DESC);

CREATE TABLE IF NOT EXISTS conversation_members (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP,
    CONSTRAINT uk_conversation_member UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_member_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conv_members_user ON conversation_members (user_id);
CREATE INDEX IF NOT EXISTS idx_conv_members_conv ON conversation_members (conversation_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    media_url VARCHAR(1000),
    media_name VARCHAR(255),
    media_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_conv_created ON messages (conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id);

CREATE TABLE IF NOT EXISTS message_status (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT uk_message_status_recipient UNIQUE (message_id, user_id),
    CONSTRAINT fk_msg_status_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_status_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_msg_status_user ON message_status (user_id, status);

CREATE TABLE IF NOT EXISTS call_history (
    id UUID PRIMARY KEY,
    caller_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    call_type VARCHAR(20) NOT NULL DEFAULT 'VIDEO',
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INTEGER DEFAULT 0,
    CONSTRAINT fk_call_caller FOREIGN KEY (caller_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_call_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_call_history_caller ON call_history (caller_id);
CREATE INDEX IF NOT EXISTS idx_call_history_receiver ON call_history (receiver_id);
