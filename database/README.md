# Table21 Database Setup

This guide explains how to set up the MySQL database for the Table21 Blackjack project.

## Requirements

- Docker Desktop
- Java 25
- Git
- The Table21 project repository

## 1. Start Docker

Open Docker Desktop and wait until the Docker engine is running.

Open a terminal in the project's root directory.

## 2. Start MySQL

Run:

```bash
docker compose up -d mysql
```

Check the container status:

```bash
docker compose ps
```

Wait until the MySQL container shows `healthy`.

## 3. Database Connection Settings

The local development database uses:

- Database: `table21`
- Username: `table21`
- Password: `table21_dev`
- Host: `127.0.0.1`
- Port: `3307`

Set these environment variables only in the server terminal and when running database integration tests. Clients use `TABLE21_SERVER_HOST` and `TABLE21_SERVER_PORT` (defaults `127.0.0.1:5555`) and never access MySQL directly.

Server settings:

```bash
export TABLE21_DB_URL="jdbc:mysql://127.0.0.1:3307/table21"
export TABLE21_DB_USER="table21"
export TABLE21_DB_PASSWORD="table21_dev"
```

These variables must be set again when opening a new terminal session.

## 4. Database Tables

The SQL migration files are located in `database/migrations/`.

They create the following tables:

- `users` — stores user accounts and password hashes.
- `wallets` — stores player chip balances.
- `game_history` — stores completed game results.
- `house_wallet` — stores the house chip balance.
- `leaderboard` — stores permanent arcade runs, separate from active accounts.

Docker automatically executes the migration scripts when initializing a new MySQL data volume.

Existing database volumes are preserved when restarting the container. Adding a new migration file does not automatically apply it to an existing database.

## 5. Verify the Database

Run:

```bash
docker compose exec mysql mysql -utable21 -ptable21_dev table21 -e "SHOW TABLES;"
```

You should see five tables, including `leaderboard` after applying V006.

## 6. Run Tests

With MySQL running and the environment variables configured, execute:

```bash
./gradlew clean test
```

A successful test run displays:

```text
BUILD SUCCESSFUL
```

## 7. Stop MySQL

To stop the database container:

```bash
docker compose stop mysql
```

To restart it later:

```bash
docker compose up -d mysql
```

Do not use `docker compose down -v` unless you intentionally want to delete the database volume and its stored data.

## Notes

- Newly registered accounts automatically receive a wallet containing 1,000 chips.
- Passwords are stored as secure hashes, not plaintext.
- Database connection settings are read from environment variables.
- The credentials above are for local development only and should not be used in production.

## House bankroll initialization

`V005__initialize_house_balance.sql` sets the house bankroll to $100,000 on fresh databases without replacing the table. For an existing development database, stop the game server and run this once from the project root:

```bash
docker compose exec -T mysql mysql -utable21 -ptable21_dev table21 < database/migrations/V005__initialize_house_balance.sql
```

This intentionally sets the current bankroll to $100,000. Do not rerun it during normal play. No volume reset is needed. Wagers credit the house and payouts debit it; there is no automatic refill. Authenticated player wallets are loaded from MySQL and saved after wagers and payouts. See [multiplayer setup](../MULTIPLAYER_SETUP.md) for authentication, reconnect testing, and crash-consistency limitations.

## Arcade leaderboard and bankruptcy (V006 / V007)

An **active account** contains credentials, a wallet, and a link to gameplay history.
An **arcade record** contains a username snapshot, highest bankroll, wins, and the
first time that peak was achieved. The arcade record survives account deletion.
New registrations create the user, $1,000 wallet, and $1,000 leaderboard record in
one transaction. Reusing a bankrupt username creates a new account/run; it does not
replace its older score. Duplicate display names are therefore possible.

V006 seeds existing accounts with the greater of $1,000 or their current wallet
balance, and counts existing winning hands. Earlier peaks cannot be reconstructed
from the current wallet and are not guessed. A later settled balance updates the
peak and timestamp only if strictly greater. Equal or lower balances retain both.
Peak profit is `peak_balance - 1000.00`. Wins count `PLAYER_WIN` history rows, so a
split can earn two wins; pushes and losses earn none.

The query displays at most 20 records ordered by peak balance descending, peak
achievement time ascending, then leaderboard ID ascending. Lower-ranked records
remain stored. No house bankroll, casino statistics, IDs, or credentials are
returned in the player leaderboard response.

V007 changes `game_history.user_id` to nullable with `ON DELETE SET NULL` and
links history to the permanent `leaderboard_id`. Both history and leaderboard
survive deletion of the user. The new unique `(round_id, leaderboard_id, hand_index)`
key remains valid even after the active user link becomes null. Existing history
is backfilled to its account's arcade record. The wallet FK remains restrictive:
the bankruptcy transaction explicitly deletes the wallet before deleting the user.
Leaderboard deletion is restricted while history references it.

At completed settlement the server saves every hand with a UUID shared by the
round, zero-based hand index, result, full wager (including double), actual gross
payout, split/double flags, and database timestamp. Blackjack uses the same rounded
payout calculation as house accounting. Unique hand keys make repeated recording
idempotent, and wins are recounted from linked history rather than incremented on
retries. No history is written while a house payout is still pending.

After wallet persistence, history writes, peak/win updates, and zero-balance account
deletion run in one transaction per player. Bankruptcy occurs only after settlement,
not when an all-in wager temporarily reduces the wallet to zero. The server retains
the score/history, removes the wallet/account, sends `BANKRUPT`, and closes the socket.
Other sockets for that account lose access on their next request. There is no recovery
or automatic account recreation; the player may explicitly register a fresh account.

House transfers and player wallet persistence still precede this transaction. They
are **not** one crash-safe transaction. A history/score/deletion failure rolls back
that player's final-recording transaction and halts the table; do not retry money
transfers. An operator must reconcile database and in-memory results before restarting.
A crash before final recording can leave a saved wallet without history/score updates;
this branch does not add durable settlement recovery or cross-server coordination.

### Apply to an existing development database

Stop the casino server, keep MySQL running, and apply these migrations **once, in order**:

```bash
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"' < database/migrations/V006__create_leaderboard.sql
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"' < database/migrations/V007__retain_game_history.sql
```

These use the container's development credentials without printing them. Do not
rerun V005, reset balances, or delete the Docker volume. Fresh databases apply all
migrations automatically. Restart `./gradlew runServer` with the server database
environment configured, then restart clients with `./gradlew run`.
