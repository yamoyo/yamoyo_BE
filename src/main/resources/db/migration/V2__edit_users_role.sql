-- 기존 profile_image_url 컬럼 삭제
ALTER TABLE users DROP COLUMN profile_image_url;

-- 새로운 profile_image_id 컬럼 추가
ALTER TABLE users ADD COLUMN profile_image_id BIGINT NULL;

-- user_role 컬럼 추가
ALTER TABLE users ADD COLUMN user_role VARCHAR(30) DEFAULT 'GUEST' NOT NULL;

-- social_id -> social_account_id 수정
ALTER TABLE social_accounts CHANGE social_id social_account_id BIGINT NOT NULL AUTO_INCREMENT;

-- social_accounts 테이블에 created_at 컬럼 추가
ALTER TABLE social_accounts ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- user_devices 테이블 fcm_id를 user_device_id 로 수정
ALTER TABLE user_devices CHANGE fcm_id user_device_id BIGINT NOT NULL AUTO_INCREMENT;

-- team_members 테이블 role 컬럼 team_role 로 수정
ALTER TABLE team_members CHANGE role team_role VARCHAR(10) NULL COMMENT 'LEADER / HOST / MEMBER';

-- user_devices 테이블에 device_name 컬럼 추가
ALTER TABLE user_devices ADD COLUMN device_name VARCHAR(255) NULL;

-- user_devices 테이블에 updated_at 컬럼을 last_login_at 으로 수정
ALTER TABLE user_devices CHANGE updated_at last_login_at DATETIME NULL COMMENT '마지막 로그인 시간';