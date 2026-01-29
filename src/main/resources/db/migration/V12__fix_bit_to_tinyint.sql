-- V12: Fix BIT columns to TINYINT
-- Issue: MySQL automatically converts TINYINT(1) to BIT, but Hibernate expects TINYINT

-- Fix team_room_setups columns (BIT -> TINYINT)
ALTER TABLE team_room_setups
    MODIFY COLUMN tool_completed TINYINT NOT NULL DEFAULT 0,
    MODIFY COLUMN rule_completed TINYINT NOT NULL DEFAULT 0,
    MODIFY COLUMN meeting_completed TINYINT NOT NULL DEFAULT 0;

-- Fix team_tool_proposals.is_approval column (BIT -> TINYINT)
ALTER TABLE team_tool_proposals
    MODIFY COLUMN is_approval TINYINT NOT NULL DEFAULT 0;
