-- V6 : 협업툴 테이블 구조 변경으로 인한 선 삭제 처리

-- FK 제약 먼저 삭제
ALTER TABLE member_tool_votes DROP FOREIGN KEY fk_member_tool_votes_template;
ALTER TABLE team_tool_proposals DROP FOREIGN KEY fk_team_tool_proposals_template;
ALTER TABLE tool_templates DROP FOREIGN KEY fk_tool_templates_category;

-- 테이블 삭제
DROP TABLE IF EXISTS member_tool_votes; -- 구조변경
DROP TABLE IF EXISTS team_tool_proposals; -- 구조변경
DROP TABLE IF EXISTS tool_templates; -- 불필요한 테이블
DROP TABLE IF EXISTS tool_categories; -- 불필요한 테이블