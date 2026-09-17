# Table21

Table21 is a virtual Blackjack casino application being developed for our CSCI 3033 group project at Middle Tennessee State University.

The project uses:

- Java
- Kotlin
- JavaFX
- Gradle
- JUnit
- MySQL

The goal is to build a Blackjack application with a graphical interface, user accounts, virtual balances, betting, game history, and eventually multiplayer/networking support.

## Current Status

The project is currently in early development.

The basic project structure is set up and the following are working:

- Java 25
- Kotlin
- JavaFX
- Gradle
- JUnit testing
- Basic JavaFX application window

## Requirements

Install Java JDK 25.

You do **not** need to install Gradle separately because the project includes the Gradle Wrapper.

## Run the Project

Clone the repository and enter the project folder:

```bash
git clone <repository-url>
cd MTSU-BlackJack
```

## Run the Tests

```bash
./gradlew test
```

## Run the Application

```bash
./gradlew run
```

A basic Table21 JavaFX window should open.

## Windows

Use:

```powershell
gradlew.bat test
gradlew.bat run
```

## Linux

Use:

```bash
./gradlew test
./gradlew run
```

## macOS

Use:

```bash
./gradlew test
./gradlew run
```

## Project Structure

```text
src/main/java/       Java game, database, and model code
src/main/kotlin/     Kotlin UI, server, and service code
src/test/            JUnit tests
database/            Database migrations
docs/                Project documentation and timesheets
```
