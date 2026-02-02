ALTER TABLE notifications
    ADD COLUMN team_room_id BIGINT NOT NULL AFTER target_id,
    ADD CONSTRAINT fk_notifications_team_room
        FOREIGN KEY (team_room_id) REFERENCES team_rooms(team_room_id)
        ON DELETE CASCADE;

ALTER TABLE notifications
    MODIFY COLUMN type VARCHAR(50) NOT NULL COMMENT 'NotificationType Enum String',
    MODIFY COLUMN title VARCHAR(100) NOT NULL,
    MODIFY COLUMN message VARCHAR(255) NOT NULL;
