-- Remove duplicate user agreements (keep the most recent one)
DELETE ua1 FROM user_agreements ua1
INNER JOIN user_agreements ua2
ON ua1.user_id = ua2.user_id
   AND ua1.terms_id = ua2.terms_id
   AND ua1.agreement_id < ua2.agreement_id;

-- Add unique constraint to prevent duplicate agreements
ALTER TABLE user_agreements
ADD CONSTRAINT uk_user_agreements_user_terms UNIQUE (user_id, terms_id);
