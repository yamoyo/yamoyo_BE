-- V8 : 협업툴 투표 테이블, 협업툴 제안 테이블 재생성

CREATE TABLE member_tool_votes (
    tool_vote_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    team_room_id BIGINT NOT NULL,
    category_id INT NOT NULL,
    tool_id INT NOT NULL,
    PRIMARY KEY (tool_vote_id),
    CONSTRAINT uk_member_tool_votes UNIQUE (member_id, tool_id),
    CONSTRAINT fk_member_tool_votes_member
        FOREIGN KEY (member_id) REFERENCES team_members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_member_tool_votes_team_room
        FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE
);

CREATE TABLE team_tool_proposals (
    proposal_id BIGINT NOT NULL AUTO_INCREMENT,
    team_room_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    category_id INT NOT NULL,
    tool_id INT NOT NULL,
    is_approval TINYINT(1) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at DATETIME NULL,
    PRIMARY KEY (proposal_id),
    CONSTRAINT fk_team_tool_proposals_team_room
        FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_tool_proposals_member
        FOREIGN KEY (requested_by) REFERENCES team_members(member_id) ON DELETE CASCADE
);