package com.db.awmd.challenge.exception;

public class AmountTransferShouldBeGreaterThanZero extends RuntimeException {

  public AmountTransferShouldBeGreaterThanZero(String message) {
    super(message);
  }
}
