DROP PROCEDURE IF EXISTS playedu_training_credits_backfill;
DELIMITER $$
CREATE PROCEDURE playedu_training_credits_backfill()
BEGIN
    DECLARE v_inserted BIGINT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS tmp_credit_backfill;
        RESIGNAL;
    END;

    DROP TEMPORARY TABLE IF EXISTS tmp_credit_backfill;
    CREATE TEMPORARY TABLE tmp_credit_backfill (
        user_id INT NOT NULL,
        hour_id INT NOT NULL,
        course_id INT NOT NULL,
        credit INT NOT NULL,
        course_title VARCHAR(255) NOT NULL,
        hour_title VARCHAR(255) NOT NULL,
        finished_at DATETIME NULL,
        PRIMARY KEY (user_id, hour_id)
    ) ENGINE=InnoDB;

    START TRANSACTION;

    IF EXISTS (
        SELECT 1
        FROM users AS u
        LEFT JOIN (
            SELECT user_id, SUM(credit) AS total_credit
            FROM user_credit_records
            GROUP BY user_id
        ) AS ledger ON ledger.user_id = u.id
        WHERE u.credit1 <> 0
            AND u.credit1 <> COALESCE(ledger.total_credit, 0)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Nonzero balances differ from existing credit ledger';
    END IF;

    INSERT INTO tmp_credit_backfill (
        user_id,
        hour_id,
        course_id,
        credit,
        course_title,
        hour_title,
        finished_at
    )
    SELECT
        r.user_id,
        r.hour_id,
        r.course_id,
        h.credit,
        c.title,
        h.title,
        MIN(COALESCE(r.finished_at, r.created_at))
    FROM user_course_hour_records AS r
    INNER JOIN course_hour AS h ON h.id = r.hour_id AND h.course_id = r.course_id
    INNER JOIN courses AS c ON c.id = r.course_id
    WHERE r.is_finished = 1
    GROUP BY r.user_id, r.hour_id, r.course_id, h.credit, c.title, h.title;

    INSERT IGNORE INTO user_credit_records (
        user_id,
        course_id,
        hour_id,
        source_type,
        credit,
        course_title,
        hour_title,
        created_at
    )
    SELECT
        user_id,
        course_id,
        hour_id,
        'COURSE_HOUR',
        credit,
        course_title,
        hour_title,
        finished_at
    FROM tmp_credit_backfill;
    SET v_inserted = ROW_COUNT();

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT user_id, SUM(credit) AS total_credit
            FROM user_credit_records
            GROUP BY user_id
            HAVING SUM(credit) > 2147483647 OR SUM(credit) < -2147483648
        ) AS totals
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Credit balance would overflow INT';
    END IF;

    UPDATE users AS u
    LEFT JOIN (
        SELECT user_id, SUM(credit) AS total_credit
        FROM user_credit_records
        GROUP BY user_id
    ) AS ledger ON ledger.user_id = u.id
    SET u.credit1 = COALESCE(ledger.total_credit, 0);

    COMMIT;
    DROP TEMPORARY TABLE IF EXISTS tmp_credit_backfill;

    SELECT
        v_inserted AS inserted_records,
        (SELECT COUNT(*) FROM user_credit_records) AS ledger_records,
        (
            SELECT COUNT(*)
            FROM users AS u
            LEFT JOIN (
                SELECT user_id, SUM(credit) AS total_credit
                FROM user_credit_records
                GROUP BY user_id
            ) AS ledger ON ledger.user_id = u.id
            WHERE u.credit1 <> COALESCE(ledger.total_credit, 0)
        ) AS inconsistent_balances;
END$$
DELIMITER ;
CALL playedu_training_credits_backfill();
DROP PROCEDURE playedu_training_credits_backfill;
