-- Add onboarding_status column to users table
ALTER TABLE users ADD COLUMN onboarding_status VARCHAR(20) DEFAULT 'TERMS_PENDING';

-- Migrate existing data based on current state
-- COMPLETED: major field is not null (profile is complete)
-- PROFILE_PENDING: agreed to all mandatory terms but major is null
-- TERMS_PENDING: has not agreed to all mandatory terms (default)
UPDATE users u SET onboarding_status =
  CASE
    WHEN u.major IS NOT NULL THEN 'COMPLETED'
    WHEN EXISTS (
      SELECT 1 FROM user_agreements ua
      JOIN terms t ON ua.terms_id = t.terms_id
      WHERE ua.user_id = u.user_id
        AND t.is_mandatory = 1
        AND t.is_active = 1
        AND ua.is_agreed = 1
      GROUP BY ua.user_id
      HAVING COUNT(DISTINCT ua.terms_id) = (
        SELECT COUNT(*) FROM terms WHERE is_mandatory = 1 AND is_active = 1
      )
    ) THEN 'PROFILE_PENDING'
    ELSE 'TERMS_PENDING'
  END;
