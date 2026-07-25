package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.PERSON_NOT_FOUND_IN_INSTITUTION;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PersonNotFoundInInstitutionException extends ApplicationException {
  public PersonNotFoundInInstitutionException() {
    super(ErrorCategory.NOT_FOUND, PERSON_NOT_FOUND_IN_INSTITUTION);
  }
}
