-- Run once after registering the huang_demo test user.

USE bank_demo;

UPDATE accounts
SET user_id = (
    SELECT id
    FROM app_users
    WHERE username = 'huang_demo'
)
WHERE account_number = 'A001';

SELECT account_number, owner_name, user_id
FROM accounts;
