-- Learner owns skill_level. Preserve values from older user rows before dropping the duplicate column.
UPDATE learners AS learner
SET skill_level = account.skill_level
FROM users AS account
WHERE learner.user_id = account.user_id
  AND learner.skill_level IS NULL
  AND account.skill_level IS NOT NULL;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_skill_level;
ALTER TABLE users DROP COLUMN skill_level;
ALTER TABLE learners DROP COLUMN created_at;
