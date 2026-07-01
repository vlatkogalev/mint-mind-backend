-- Rename and retype grade columns on the coins table

ALTER TABLE coins RENAME COLUMN estimated_grade       TO grade_name;
ALTER TABLE coins RENAME COLUMN estimated_grade_value TO grade_abbreviation;

-- grade_code was stored as VARCHAR; replace with an INTEGER column
ALTER TABLE coins ADD COLUMN grade_numeric INTEGER;

-- Best-effort data migration: grade_code historically held values like "AU-58"
-- or a bare integer string; attempt to parse the integer part
UPDATE coins
SET grade_numeric = NULLIF(
    regexp_replace(COALESCE(grade_code, ''), '[^0-9]', '', 'g'),
    ''
)::INTEGER
WHERE grade_code IS NOT NULL;

ALTER TABLE coins DROP COLUMN grade_code;

-- grade_confidence column name is already correct, no change needed
