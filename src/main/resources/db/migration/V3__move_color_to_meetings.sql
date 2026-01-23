-- meetings 테이블에 color 컬럼 추가
ALTER TABLE meetings
ADD COLUMN color VARCHAR(20) NOT NULL COMMENT 'PURPLE / YELLOW / PINK / BLUE / SKYBLUE / RED / ORANGE';

-- meetings 테이블에 description 컬럼 추가
ALTER TABLE meetings
ADD COLUMN description VARCHAR(255) NULL;

-- meeting_series 테이블에서 color 컬럼 삭제
ALTER TABLE meeting_series DROP COLUMN color;
