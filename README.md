# Table21

Table21 is a virtual Blackjack casino application being developed for our CSCI 3033 group project at Middle Tennessee State University.

The project uses:

- Java
- Kotlin
- JavaFX
- Gradle
- JUnit
- MySQL
- Docker

The goal is to build a Blackjack application with a graphical interface, user accounts, virtual balances, betting, game history, and multiplayer/networking support.

## Current Status

The basic project structure is set up and the following are working:

- Java 25
- Kotlin
- JavaFX
- Gradle
- JUnit testing
- MySQL database
- User accounts and persistent balances
- Blackjack game logic
- Multiplayer client/server networking
- Virtual betting and house balance
- Admin interface
- Player leaderboard and game history

## Requirements

Install:

- Java JDK 25
- Docker Desktop

You do **not** need to install Gradle separately because the project includes the Gradle Wrapper.

## Clone the Project

```bash
git clone <repository-url>
cd MTSU-BlackJack
```

## Start the Database

Make sure Docker Desktop is running.

From the project folder:

```bash
docker compose up -d
```

Check that MySQL is running:

```bash
docker compose ps
```

## Configure the Database

The Table21 server needs the database connection settings.

### macOS / Linux

```bash
export TABLE21_DB_URL="jdbc:mysql://127.0.0.1:3307/table21"
export TABLE21_DB_USER="table21"
export TABLE21_DB_PASSWORD="table21_dev"
```

### Windows PowerShell

```powershell
$env:TABLE21_DB_URL = "jdbc:mysql://127.0.0.1:3307/table21"
$env:TABLE21_DB_USER = "table21"
$env:TABLE21_DB_PASSWORD = "table21_dev"
```

These variables only need to be configured on the computer running the Table21 server.

## Run the Tests

### macOS / Linux

```bash
./gradlew clean test
```

### Windows

```powershell
gradlew.bat clean test
```

## Start the Server

Start the server before opening the game clients.

### macOS / Linux

```bash
./gradlew runServer
```

### Windows

```powershell
gradlew.bat runServer
```

Leave this terminal open while Table21 is running.

The server uses port `5555`.

## Run the Application

Open a **new terminal** in the project folder.

### macOS / Linux

```bash
./gradlew run
```

### Windows

```powershell
gradlew.bat run
```

The Table21 JavaFX application should open.

For local testing, the client automatically connects to:

```text
127.0.0.1:5555
```

You can open multiple terminals and run:

```bash
./gradlew run
```

multiple times to test several player accounts at the same Blackjack table.

## Connect From Another Computer

The computer running the server must remain running.

Find the server computer's IP address and start the client on another computer with:

### macOS / Linux

```bash
TABLE21_SERVER_HOST=<server-ip> ./gradlew run
```

Example:

```bash
TABLE21_SERVER_HOST=192.168.1.25 ./gradlew run
```

### Windows PowerShell

```powershell
$env:TABLE21_SERVER_HOST = "192.168.1.25"
gradlew.bat run
```

The client defaults to port `5555`.

A different port can be configured with:

```bash
TABLE21_SERVER_PORT=5555 ./gradlew run
```

Only the server computer needs access to MySQL. Other players only connect to the Table21 server.

## Basic Run Order

```text
1. Start Docker Desktop
2. Run: docker compose up -d
3. Configure the database environment variables
4. Run: ./gradlew runServer
5. Open another terminal
6. Run: ./gradlew run
7. Login or create an account
8. Join the Blackjack table
```

## Stop the Database

When finished:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the stored database data.

## Project Structure

```text
src/main/java/       Java game, database, and model code
src/main/kotlin/     Kotlin UI, client, server, and service code
src/test/            JUnit tests
database/            Database migrations and setup
docs/                Project documentation and timesheets
```
