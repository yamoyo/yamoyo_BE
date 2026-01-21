-- =============================================================================
-- Flyway Migration
-- 파일명 규칙: V{버전}__{설명}.sql (예: V2__add_column.sql)
-- 주의: 이미 적용된 마이그레이션 파일은 수정하지 마세요.
--       스키마 변경이나 데이터 추가가 필요하면 새 버전 파일을 생성하세요.
-- =============================================================================

CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(255) NULL,
    major VARCHAR(50) NULL,
    mbti CHAR(4) NULL,
    is_alarm_on TINYINT(1) NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE team_rooms (
    team_room_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    deadline DATETIME NOT NULL,
    lifecycle VARCHAR(20) NULL COMMENT 'ACTIVE / DELETE / ARCHIVE',
    workflow VARCHAR(20) NULL COMMENT 'PENDING / LEADER_SELECTION / SETUP / COMPLETED',
    banner_image_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (team_room_id)
);

CREATE TABLE team_members (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    team_room_id BIGINT NOT NULL,
    role VARCHAR(10) NULL COMMENT 'LEADER / HOST / MEMBER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_team_members_user_room UNIQUE (user_id, team_room_id),
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE team_room_setups (
    setup_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    tool_completed TINYINT(1) NOT NULL DEFAULT 0,
    rule_completed TINYINT(1) NOT NULL DEFAULT 0,
    meeting_completed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NULL COMMENT 'Created when meeting setup completed',
    deadline DATETIME NULL COMMENT 'created_at + 6 hours',
    PRIMARY KEY (setup_id),
    CONSTRAINT fk_team_room_setups_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE timepicks (
    timepick_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / FINALIZED',
    deadline DATETIME NOT NULL COMMENT 'Leader confirmed + 3 hours',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalized_at DATETIME NULL,
    PRIMARY KEY (timepick_id),
    CONSTRAINT uk_timepicks_team_room UNIQUE (team_room_id),
    CONSTRAINT fk_timepicks_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE timepick_participants (
    timepick_participant_id BIGINT NOT NULL AUTO_INCREMENT,
    timepick_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUBMITTED / EXPIRED',
    preferred_block VARCHAR(20) NULL COMMENT 'BLOCK_08_12 / BLOCK_12_16 / BLOCK_16_20 / BLOCK_20_24',
    availability_mon INT UNSIGNED NOT NULL DEFAULT 0,
    availability_tue INT UNSIGNED NOT NULL DEFAULT 0,
    availability_wed INT UNSIGNED NOT NULL DEFAULT 0,
    availability_thu INT UNSIGNED NOT NULL DEFAULT 0,
    availability_fri INT UNSIGNED NOT NULL DEFAULT 0,
    availability_sat INT UNSIGNED NOT NULL DEFAULT 0,
    availability_sun INT UNSIGNED NOT NULL DEFAULT 0,
    submitted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (timepick_participant_id),
    CONSTRAINT uk_timepick_participants UNIQUE (timepick_id, user_id),
    CONSTRAINT fk_timepick_participants_timepick FOREIGN KEY (timepick_id) REFERENCES timepicks(timepick_id) ON DELETE CASCADE,
    CONSTRAINT fk_timepick_participants_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE user_timepick_defaults (
    user_timepick_default_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    preferred_block VARCHAR(20) NULL COMMENT 'BLOCK_08_12 / BLOCK_12_16 / BLOCK_16_20 / BLOCK_20_24',
    availability_mon INT UNSIGNED NOT NULL DEFAULT 0,
    availability_tue INT UNSIGNED NOT NULL DEFAULT 0,
    availability_wed INT UNSIGNED NOT NULL DEFAULT 0,
    availability_thu INT UNSIGNED NOT NULL DEFAULT 0,
    availability_fri INT UNSIGNED NOT NULL DEFAULT 0,
    availability_sat INT UNSIGNED NOT NULL DEFAULT 0,
    availability_sun INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_timepick_default_id),
    CONSTRAINT uk_user_timepick_defaults_user UNIQUE (user_id),
    CONSTRAINT fk_user_timepick_defaults_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE meeting_series (
    meeting_series_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    meeting_type VARCHAR(30) NOT NULL COMMENT 'INITIAL_REGULAR / ADDITIONAL_RECURRING / ADDITIONAL_ONE_TIME',
    day_of_week VARCHAR(3) NOT NULL COMMENT 'MON / TUE / WED / THU / FRI / SAT / SUN',
    default_start_time TIME NOT NULL,
    default_duration_minutes INT NOT NULL DEFAULT 60,
    creator_name VARCHAR(50) NOT NULL COMMENT 'Creator name snapshot',
    color VARCHAR(20) NOT NULL COMMENT 'PURPLE / YELLOW / PINK / BLUE / SKYBLUE / RED / ORANGE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_series_id),
    CONSTRAINT fk_meeting_series_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE meetings (
    meeting_id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_series_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    location VARCHAR(255) NULL,
    start_time DATETIME NOT NULL COMMENT 'Stored in UTC',
    duration_minutes INT NOT NULL DEFAULT 60,
    is_individually_modified TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_id),
    CONSTRAINT fk_meetings_series FOREIGN KEY (meeting_series_id) REFERENCES meeting_series(meeting_series_id) ON DELETE CASCADE
);

CREATE TABLE meeting_participants (
    meeting_participant_id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_participant_id),
    CONSTRAINT uk_meeting_participants UNIQUE (meeting_id, user_id),
    CONSTRAINT fk_meeting_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_participants_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE social_accounts (
    social_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(10) NULL COMMENT 'GOOGLE / KAKAO / APPLE',
    provider_id VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    PRIMARY KEY (social_id),
    CONSTRAINT uk_social_accounts_provider UNIQUE (provider, provider_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE user_devices (
    fcm_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(512) NULL,
    device_type VARCHAR(20) NULL COMMENT 'IOS / ANDROID / WEB',
    updated_at DATETIME NULL COMMENT 'For skipping inactive devices',
    PRIMARY KEY (fcm_id),
    CONSTRAINT uk_fcm_token UNIQUE (fcm_token),
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE banned_team_members (
    ban_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    banned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ban_id),
    CONSTRAINT uk_banned_team_members_user_room UNIQUE (user_id, team_room_id),
    CONSTRAINT fk_banned_team_members_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE,
    CONSTRAINT fk_banned_team_members_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE tool_categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(20) NULL,
    PRIMARY KEY (category_id)
);

CREATE TABLE tool_templates (
    tool_template_id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    tool_name VARCHAR(255) NULL,
    PRIMARY KEY (tool_template_id),
    CONSTRAINT fk_tool_templates_category FOREIGN KEY (category_id) REFERENCES tool_categories(category_id) ON DELETE CASCADE
);

CREATE TABLE rule_templates (
    rule_template_id BIGINT NOT NULL AUTO_INCREMENT,
    rule_content VARCHAR(255) NULL,
    PRIMARY KEY (rule_template_id)
);

CREATE TABLE team_tools (
    tool_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    tool_name VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tool_id),
    CONSTRAINT fk_team_tools_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE team_rules (
    rule_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    content VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_id),
    CONSTRAINT fk_team_rules_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE member_tool_votes (
    tool_vote_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    team_room_id BIGINT NOT NULL,
    tool_template_id BIGINT NOT NULL,
    PRIMARY KEY (tool_vote_id),
    CONSTRAINT uk_member_tool_votes UNIQUE (member_id, tool_template_id),
    CONSTRAINT fk_member_tool_votes_member FOREIGN KEY (member_id) REFERENCES team_members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_tool_votes_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_tool_votes_template FOREIGN KEY (tool_template_id) REFERENCES tool_templates(tool_template_id) ON DELETE CASCADE
);

CREATE TABLE member_rule_votes (
    rule_vote_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    team_room_id BIGINT NOT NULL,
    rule_template_id BIGINT NOT NULL,
    is_agree TINYINT(1) NULL,
    PRIMARY KEY (rule_vote_id),
    CONSTRAINT uk_member_rule_votes UNIQUE (member_id, rule_template_id),
    CONSTRAINT fk_member_rule_votes_member FOREIGN KEY (member_id) REFERENCES team_members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_rule_votes_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_rule_votes_template FOREIGN KEY (rule_template_id) REFERENCES rule_templates(rule_template_id) ON DELETE CASCADE
);

CREATE TABLE team_tool_proposals (
    proposal_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    tool_template_id BIGINT NOT NULL,
    is_approval TINYINT(1) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at DATETIME NULL,
    PRIMARY KEY (proposal_id),
    CONSTRAINT fk_team_tool_proposals_team_room FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_tool_proposals_member FOREIGN KEY (requested_by) REFERENCES team_members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_tool_proposals_template FOREIGN KEY (tool_template_id) REFERENCES tool_templates(tool_template_id) ON DELETE CASCADE
);

CREATE TABLE terms (
    terms_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NULL,
    content TEXT NULL,
    version VARCHAR(50) NULL,
    is_mandatory TINYINT(1) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (terms_id)
);

CREATE TABLE user_agreements (
    agreement_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    terms_id BIGINT NOT NULL,
    is_agreed TINYINT(1) NULL,
    agreed_at DATETIME NULL,
    PRIMARY KEY (agreement_id),
    CONSTRAINT fk_user_agreements_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_agreements_terms FOREIGN KEY (terms_id) REFERENCES terms(terms_id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_id BIGINT NULL,
    type VARCHAR(30) NULL COMMENT 'Notification type',
    title VARCHAR(100) NULL,
    message VARCHAR(255) NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expiry_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_user UNIQUE (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);