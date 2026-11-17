ALTER TABLE user_accounts ADD COLUMN biography VARCHAR(500);
ALTER TABLE user_accounts ADD COLUMN course_name VARCHAR(180);
ALTER TABLE user_accounts ADD COLUMN year_of_study INTEGER;
ALTER TABLE user_accounts ADD COLUMN profile_photo_key VARCHAR(255);
ALTER TABLE user_accounts ADD COLUMN profile_photo_media_type VARCHAR(80);
ALTER TABLE user_accounts ADD CONSTRAINT ck_year_of_study
    CHECK (year_of_study IS NULL OR year_of_study BETWEEN 1 AND 12);
