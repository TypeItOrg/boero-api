package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_ASSIGNMENT_NOT_ALLOWED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleAssignmentNotAllowedException extends ApplicationException {

  public RoleAssignmentNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, ROLE_ASSIGNMENT_NOT_ALLOWED);
  }
}
