-- V12: Fix BIT columns to TINYINT

-- Fix team_tool_proposals.is_approval column (BIT -> TINYINT)
ALTER TABLE team_tool_proposals
    MODIFY COLUMN is_approval TINYINT NOT NULL DEFAULT 0;
