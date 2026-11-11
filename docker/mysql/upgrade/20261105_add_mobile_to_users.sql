ALTER TABLE users
    ADD COLUMN mobile VARCHAR(11) DEFAULT NULL COMMENT '手机号码' AFTER email;

CREATE UNIQUE INDEX uk_users_mobile ON users (mobile);
