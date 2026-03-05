-- requested_by FK를 team_members.member_id → users.user_id로 변경
-- 현재 코드가 userId를 저장하고 있으므로 DB FK를 코드에 맞춤

ALTER TABLE team_tool_proposals DROP FOREIGN KEY fk_team_tool_proposals_member;

ALTER TABLE team_tool_proposals
    ADD CONSTRAINT fk_team_tool_proposals_user
        FOREIGN KEY (requested_by) REFERENCES users (user_id) ON DELETE CASCADE;
