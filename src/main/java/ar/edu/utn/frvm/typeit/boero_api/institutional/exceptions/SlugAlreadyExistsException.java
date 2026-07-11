package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.SLUG_ALREADY_EXISTS;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.FieldConflictException;

public class SlugAlreadyExistsException extends FieldConflictException {
  public SlugAlreadyExistsException() {
    super("slug", SLUG_ALREADY_EXISTS);
  }
}
