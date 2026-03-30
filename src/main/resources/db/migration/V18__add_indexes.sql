-- =====================================================
-- V18: 조회 성능 개선을 위한 인덱스 추가
-- =====================================================

-- team_members: user_id 기반 팀룸 목록 조회
CREATE INDEX idx_team_members_user_id
    ON team_members (user_id);

-- team_members: team_room_id 기반 멤버 조회
CREATE INDEX idx_team_members_team_room_id
    ON team_members (team_room_id);

-- member_tool_votes: 카테고리별 득표 집계
CREATE INDEX idx_member_tool_votes_team_room_category
    ON member_tool_votes (team_room_id, category_id);

-- member_tool_votes: 팀룸 전체 투표 조회 / 참여현황
CREATE INDEX idx_member_tool_votes_team_room_id
    ON member_tool_votes (team_room_id);

-- member_rule_votes: 팀룸 전체 투표 조회
CREATE INDEX idx_member_rule_votes_team_room_id
    ON member_rule_votes (team_room_id);

-- team_rooms: 스케줄러용 (lifecycle + deadline)
CREATE INDEX idx_team_rooms_lifecycle_deadline
    ON team_rooms (lifecycle, deadline);

-- team_room_setups: team_room_id 기반 셋업 조회
CREATE INDEX idx_team_room_setups_team_room_id
    ON team_room_setups (team_room_id);

-- team_room_setups: 스케줄러(findExpiredSetups)에서 deadline < :now 조건 필터링
CREATE INDEX idx_team_room_setups_deadline
    ON team_room_setups (deadline);

-- team_rules: team_room_id 기반 규칙 조회
CREATE INDEX idx_team_rules_team_room_id
    ON team_rules (team_room_id);

-- team_tools: team_room_id 기반 협업툴 조회
CREATE INDEX idx_team_tools_team_room_id
    ON team_tools (team_room_id);

-- team_tool_proposals: 팀룸별 대기중 제안 조회
CREATE INDEX idx_team_tool_proposals_team_room_approval
    ON team_tool_proposals (team_room_id, is_approval);

-- team_tool_proposals: 카테고리+툴 기반 중복 제안 확인
CREATE INDEX idx_team_tool_proposals_category_tool
    ON team_tool_proposals (team_room_id, category_id, tool_id, is_approval);

-- banned_team_members: team_room_id 선행 컬럼 조회 (기존 UK는 user_id 선행)
CREATE INDEX idx_banned_team_members_team_room_user
    ON banned_team_members (team_room_id, user_id);

-- team_members: team_room_id + user_id 복합 조회 (findByTeamRoomIdAndUserId - 전 도메인에서 빈번히 호출)
CREATE INDEX idx_team_members_team_room_user
    ON team_members (team_room_id, user_id);

-- member_rule_votes: team_room_id + member_id 복합 조회 (countDistinctRulesVotedByMember, findMemberIdsWhoCompletedAllRules)
CREATE INDEX idx_member_rule_votes_team_room_member
    ON member_rule_votes (team_room_id, member_id);
