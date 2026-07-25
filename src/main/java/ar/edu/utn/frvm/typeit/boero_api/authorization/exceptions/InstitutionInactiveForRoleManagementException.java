package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InstitutionInactiveForRoleManagementException extends ApplicationException {

  public InstitutionInactiveForRoleManagementException() {
    super(ErrorCategory.CONFLICT, "La institución está inactiva.");
  }
}
