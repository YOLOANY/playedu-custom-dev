ALTER TABLE users
    MODIFY COLUMN id_card VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '身份证号密文',
    ADD COLUMN id_card_bidx CHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '身份证号盲索引' AFTER id_card,
    ADD INDEX idx_users_id_card_bidx (id_card_bidx);
