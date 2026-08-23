package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.PERSON_NOT_FOUND;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PersonNotFoundException extends ApplicationException {
  public PersonNotFoundException() {
    super(ErrorCategory.NOT_FOUND, PERSON_NOT_FOUND);
  }
}
