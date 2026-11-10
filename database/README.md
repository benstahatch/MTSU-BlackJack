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

Set these environment variables in your terminal:

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

Docker automatically executes the migration scripts when initializing a new MySQL data volume.

Existing database volumes are preserved when restarting the container. Adding a new migration file does not automatically apply it to an existing database.

## 5. Verify the Database

Run:

```bash
docker compose exec mysql mysql -utable21 -ptable21_dev table21 -e "SHOW TABLES;"
```

You should see the four database tables.

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
