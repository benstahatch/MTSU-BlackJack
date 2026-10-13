package com.mtsu.table21.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalletTest {

  @Test
  void newWalletStartsWith1000Chips() {

    Wallet wallet = new Wallet();

    assertEquals(
        1000.00,
        wallet.getBalance(),
        0.001);
  }

  @Test
  void walletCanLoadSavedBalance() {

    Wallet wallet = new Wallet(425.00);

    assertEquals(
        425.00,
        wallet.getBalance(),
        0.001);
  }

  @Test
  void chipsCanBeAdded() {

    Wallet wallet = new Wallet(500.00);

    wallet.addChips(100.00);

    assertEquals(
        600.00,
        wallet.getBalance(),
        0.001);
  }

  @Test
  void chipsCanBeRemoved() {

    Wallet wallet = new Wallet(500.00);

    wallet.removeChips(125.00);

    assertEquals(
        375.00,
        wallet.getBalance(),
        0.001);
  }

  @Test
  void cannotSpendMoreChipsThanAvailable() {

    Wallet wallet = new Wallet(50.00);

    assertThrows(
        IllegalStateException.class,
        () -> wallet.removeChips(75.00));
  }
}
