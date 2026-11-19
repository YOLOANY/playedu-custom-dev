SET @schema_name = DATABASE();

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE course_hour ADD COLUMN credit INT NOT NULL DEFAULT 0 COMMENT ''课时学分'' AFTER duration',
        'SELECT ''course_hour.credit already exists'' AS status'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'course_hour'
        AND COLUMN_NAME = 'credit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user_credit_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '学员ID',
    course_id INT NOT NULL COMMENT '课程ID',
    hour_id INT NOT NULL COMMENT '课时ID',
    source_type VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COURSE_HOUR' COMMENT '来源类型',
    credit INT NOT NULL DEFAULT 0 COMMENT '学分快照',
    course_title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '课程标题快照',
    hour_title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '课时标题快照',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_credit_user_hour (user_id, hour_id),
    KEY idx_user_credit_user_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员学分流水表';

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE users ADD INDEX idx_users_credit_id (credit1, id)',
        'SELECT ''users.idx_users_credit_id already exists'' AS status'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'users'
        AND INDEX_NAME = 'idx_users_credit_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
