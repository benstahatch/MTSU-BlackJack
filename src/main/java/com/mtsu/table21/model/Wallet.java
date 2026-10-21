package com.mtsu.table21.model;

public class Wallet {

  // New players begin with 1000 virtual chips
  public static final double DEFAULT_STARTING_BALANCE = 1000.00;

  // Current number of virtual chips
  private double balance;

  // Default constructor
  public Wallet() {
    this(DEFAULT_STARTING_BALANCE);
  }

  // Allows us to load a saved balance later from MySQL
  public Wallet(double startingBalance) {

    if (startingBalance < 0) {
      throw new IllegalArgumentException(
          "Starting balance cannot be negative");
    }

    this.balance = startingBalance;
  }

  // Return current chip balance
  public double getBalance() {
    return balance;
  }

  // Check whether the player has enough chips
  public boolean canAfford(double amount) {
    return amount > 0 && amount <= balance;
  }

  // Add chips
  public void addChips(double amount) {

    if (amount <= 0) {
      throw new IllegalArgumentException(
          "Chip amount must be greater than 0");
    }

    balance += amount;
  }

  // Remove chips
  public void removeChips(double amount) {

    if (amount <= 0) {
      throw new IllegalArgumentException(
          "Chip amount must be greater than 0");
    }

    if (amount > balance) {
      throw new IllegalStateException(
          "Insufficient chips");
    }

    balance -= amount;
  }
}
