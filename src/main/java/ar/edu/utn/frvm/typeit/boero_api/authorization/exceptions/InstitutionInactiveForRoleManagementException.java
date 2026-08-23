package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.INSTITUTION_INACTIVE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InstitutionInactiveForRoleManagementException extends ApplicationException {

  public InstitutionInactiveForRoleManagementException() {
    super(ErrorCategory.CONFLICT, INSTITUTION_INACTIVE);
  }
}
