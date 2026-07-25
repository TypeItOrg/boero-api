package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.INSTITUTION_NOT_FOUND;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InstitutionNotFoundException extends ApplicationException {
  public InstitutionNotFoundException() {
    super(ErrorCategory.NOT_FOUND, INSTITUTION_NOT_FOUND);
  }
}
