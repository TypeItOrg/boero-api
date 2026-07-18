package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.USER_ALREADY_EXISTS;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.FieldConflictException;

public class UserAlreadyExistsException extends FieldConflictException {
  public UserAlreadyExistsException() {
    super("documentNumber", USER_ALREADY_EXISTS);
  }
}
