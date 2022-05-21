package com.db.awmd.challenge.exception;

public class AccountDoesNotExistException extends RuntimeException {

  public AccountDoesNotExistException(String message) {
    super(message);
  }
}
