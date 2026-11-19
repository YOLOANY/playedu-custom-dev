SELECT
    COUNT(*) AS inconsistent_balances
FROM users AS u
LEFT JOIN (
    SELECT user_id, SUM(credit) AS total_credit
    FROM user_credit_records
    GROUP BY user_id
) AS ledger ON ledger.user_id = u.id
WHERE u.credit1 <> COALESCE(ledger.total_credit, 0);

SELECT
    COUNT(*) AS missing_known_credit_records
FROM (
    SELECT DISTINCT user_id, hour_id, course_id
    FROM user_course_hour_records
    WHERE is_finished = 1
) AS finished
INNER JOIN course_hour AS h ON h.id = finished.hour_id AND h.course_id = finished.course_id
INNER JOIN courses AS c ON c.id = finished.course_id
LEFT JOIN user_credit_records AS ledger
    ON ledger.user_id = finished.user_id AND ledger.hour_id = finished.hour_id
WHERE ledger.id IS NULL;

SELECT
    COUNT(*) AS duplicate_ledger_groups
FROM (
    SELECT user_id, hour_id
    FROM user_credit_records
    GROUP BY user_id, hour_id
    HAVING COUNT(*) > 1
) AS duplicates;

SELECT
    COUNT(*) AS ledger_records,
    COALESCE(SUM(credit), 0) AS ledger_credit
FROM user_credit_records;
