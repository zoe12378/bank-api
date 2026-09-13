-- Local demonstration only: grant huang_demo permission to use /api/admin/**.
-- Log out and log in again after running this script so the new JWT contains ROLE_ADMIN.

USE bank_demo;

UPDATE app_users
SET role = 'ROLE_ADMIN'
WHERE username = 'huang_demo';

SELECT id, username, role
FROM app_users
WHERE username = 'huang_demo';
