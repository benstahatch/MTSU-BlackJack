TABLE21 - ADD AN ADMIN ACCOUNT
================================

IMPORTANT: How this project currently grants admin access
---------------------------------------------------------
The current Table21 code does NOT store an admin flag or role in the users
 table. The users table stores the account ID, username, password hash, and
 creation time. Instead, the server grants admin access to account IDs listed
 in the TABLE21_ADMIN_USER_IDS environment variable.

That means you must first create a normal account, look up its database ID,
then add that ID to the server's environment variable and restart the server.
Do not add an is_admin column or manually insert a password into the users
table; the current application does not read an admin column, and passwords
must be created through the app so they are hashed correctly.

STEP 1 - Create the account
--------------------------
1. Start the Table21 application.
2. Use the Register/Create Account screen to create the account that should
   become an administrator.
3. Remember the exact username.

STEP 2 - Find the account's user ID
----------------------------------
With Docker/MySQL running, open a terminal in the project folder and run:

    docker compose exec mysql mysql -utable21 -ptable21_dev table21

At the MySQL prompt, run this query (replace the username):

    SELECT id, username FROM users WHERE username = 'your_admin_username';

Example result:

    +----+---------------+
    | id | username      |
    +----+---------------+
    |  3 | casino_admin  |
    +----+---------------+

In this example, the administrator's user ID is 3. Use the ID from your own
query, not the example ID. Exit MySQL with:

    exit;

STEP 3 - Add the ID to the server environment
---------------------------------------------
The server checks TABLE21_ADMIN_USER_IDS during login. Set it in the terminal
that you use to start the SERVER (not just the client).

Windows PowerShell - current terminal only:

    $env:TABLE21_ADMIN_USER_IDS = "3"

Windows Command Prompt - current terminal only:

    set TABLE21_ADMIN_USER_IDS=3

Linux/macOS - current terminal only:

    export TABLE21_ADMIN_USER_IDS="3"

Replace 3 with the real database ID.

To grant admin access to more than one account, separate IDs with commas.
For example:

    TABLE21_ADMIN_USER_IDS=3,7,12

PowerShell example for multiple admins:

    $env:TABLE21_ADMIN_USER_IDS = "3,7,12"

STEP 4 - Restart the server and sign in
---------------------------------------
1. Stop the Table21 server completely.
2. Open/use the server terminal where TABLE21_ADMIN_USER_IDS is set.
3. Start the server again.
4. Log out of the app if necessary, then sign in with the account you granted
   admin access to.
5. The server should identify that account as ADMIN and allow the admin views.

The environment variable is read by the server when a user logs in. Restarting
the server is required after changing the variable. If the account was already
logged in, log out and sign in again so the server checks its access again.

OPTIONAL - Keep the setting for future PowerShell terminals
-----------------------------------------------------------
To save the variable for future Windows sessions, run this in PowerShell:

    [Environment]::SetEnvironmentVariable('TABLE21_ADMIN_USER_IDS', '3', 'User')

Open a NEW terminal after running that command. For multiple admins, use a
comma-separated value such as '3,7,12'. Restart the server after changing it.

SECURITY NOTES
--------------
- Only grant admin access to accounts you trust.
- Do not share administrator passwords.
- Do not commit passwords or private production credentials to Git.
- The TABLE21_ADMIN_USER_IDS setting belongs on the server machine/process.
- Removing an ID from the variable revokes that account's admin access after
  the server is restarted and the account signs in again.

QUICK CHECK
-----------
To verify the ID before granting access, run:

    SELECT id, username FROM users ORDER BY id;

Remember: in the current implementation, admin membership is controlled by
TABLE21_ADMIN_USER_IDS, not by a database role column.
