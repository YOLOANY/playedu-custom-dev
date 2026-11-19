SELECT
    COUNT(*) AS finished_rows,
    COALESCE(SUM(h.id IS NULL OR c.id IS NULL OR h.course_id <> r.course_id), 0) AS orphan_rows
FROM user_course_hour_records AS r
LEFT JOIN course_hour AS h ON h.id = r.hour_id
LEFT JOIN courses AS c ON c.id = r.course_id
WHERE r.is_finished = 1;

SELECT
    COUNT(*) AS duplicate_groups,
    COALESCE(SUM(row_count - 1), 0) AS duplicate_rows
FROM (
    SELECT user_id, hour_id, COUNT(*) AS row_count
    FROM user_course_hour_records
    WHERE is_finished = 1
    GROUP BY user_id, hour_id
    HAVING COUNT(*) > 1
) AS duplicates;

SELECT
    COUNT(*) AS inconsistent_nonzero_balances
FROM users AS u
LEFT JOIN (
    SELECT user_id, SUM(credit) AS total_credit
    FROM user_credit_records
    GROUP BY user_id
) AS ledger ON ledger.user_id = u.id
WHERE u.credit1 <> 0
    AND u.credit1 <> COALESCE(ledger.total_credit, 0);

SELECT
    r.user_id,
    r.hour_id,
    r.course_id,
    r.finished_at
FROM user_course_hour_records AS r
LEFT JOIN course_hour AS h ON h.id = r.hour_id AND h.course_id = r.course_id
LEFT JOIN courses AS c ON c.id = r.course_id
WHERE r.is_finished = 1
    AND (h.id IS NULL OR c.id IS NULL)
ORDER BY r.user_id, r.hour_id
LIMIT 100;

SELECT
    SUM(total_credit > 2147483647 OR total_credit < -2147483648) AS overflow_users
FROM (
    SELECT r.user_id, COALESCE(SUM(h.credit), 0) AS total_credit
    FROM (
        SELECT DISTINCT user_id, hour_id, course_id
        FROM user_course_hour_records
        WHERE is_finished = 1
    ) AS r
    INNER JOIN course_hour AS h ON h.id = r.hour_id AND h.course_id = r.course_id
    GROUP BY r.user_id
) AS totals;
