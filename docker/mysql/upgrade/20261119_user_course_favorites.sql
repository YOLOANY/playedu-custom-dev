CREATE TABLE IF NOT EXISTS user_course_favorites (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL DEFAULT 0 COMMENT '学员ID',
    course_id INT NOT NULL DEFAULT 0 COMMENT '课程ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_course_favorite (user_id, course_id),
    KEY idx_user_favorite_user_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员课程收藏表';
