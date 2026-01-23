-- V7 : 협업툴/규칙 테이블 리팩토링

-- 1. team_tools 재구성
ALTER TABLE team_tools
    DROP FOREIGN KEY fk_team_tools_team_room;

ALTER TABLE team_tools
    CHANGE tool_id team_tool_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE team_tools
    DROP COLUMN tool_name;

ALTER TABLE team_tools
    ADD COLUMN category_id INT NOT NULL AFTER team_room_id,
    ADD COLUMN tool_id INT NOT NULL AFTER category_id;

ALTER TABLE team_tools
    ADD CONSTRAINT fk_team_tools_team_room
        FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE;

-- =======================================================================================
-- 2. team_rules 수정
ALTER TABLE team_rules
    CHANGE rule_id team_rule_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE team_rules
    ADD COLUMN updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- =======================================================================================
-- 3. rule_templates 수정
ALTER TABLE rule_templates
    CHANGE rule_template_id rule_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE rule_templates
    CHANGE rule_content content VARCHAR(255) NULL;

-- =======================================================================================
-- 4. member_rule_votes FK 재연결
ALTER TABLE member_rule_votes
    DROP FOREIGN KEY fk_member_rule_votes_template;

ALTER TABLE member_rule_votes
    CHANGE rule_template_id rule_id BIGINT NOT NULL;

ALTER TABLE member_rule_votes
    ADD CONSTRAINT fk_member_rule_votes_template
        FOREIGN KEY (rule_id) REFERENCES rule_templates(rule_id) ON DELETE CASCADE;