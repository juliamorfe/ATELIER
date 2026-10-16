CREATE TABLE app_user (
 email VARCHAR(254) PRIMARY KEY,
 password_hash VARCHAR(100) NOT NULL
);
CREATE TABLE user_closet (
 owner_email VARCHAR(254) PRIMARY KEY REFERENCES app_user(email) ON DELETE CASCADE,
 snapshot TEXT NOT NULL
);
CREATE TABLE garment_image (
 filename VARCHAR(40) PRIMARY KEY,
 owner_email VARCHAR(254) NOT NULL REFERENCES app_user(email) ON DELETE CASCADE,
 content BYTEA NOT NULL
);
CREATE INDEX garment_image_owner ON garment_image(owner_email);
