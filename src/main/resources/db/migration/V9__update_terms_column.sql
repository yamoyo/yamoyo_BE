-- terms table is_active column, type column 추가하기
ALTER TABLE terms
    ADD COLUMN terms_type VARCHAR(50) NOT NULL COMMENT '약관 종류 (SERVICE, PRIVACY, etc.)' AFTER terms_id,
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER is_mandatory;
