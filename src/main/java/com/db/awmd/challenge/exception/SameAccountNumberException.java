package com.db.awmd.challenge.exception;

public class SameAccountNumberException extends RuntimeException {

  public SameAccountNumberException(String message) {
    super(message);
  }
}
