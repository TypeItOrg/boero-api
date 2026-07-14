package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PLATFORM_ACCOUNT_EMAIL_ALREADY_EXISTS;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.FieldConflictException;

public class PlatformAccountEmailAlreadyExistsException extends FieldConflictException {
  public PlatformAccountEmailAlreadyExistsException() {
    super("email", PLATFORM_ACCOUNT_EMAIL_ALREADY_EXISTS);
  }
}
