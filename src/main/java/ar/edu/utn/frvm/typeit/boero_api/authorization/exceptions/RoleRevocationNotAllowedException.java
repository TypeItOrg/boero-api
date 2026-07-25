package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleRevocationNotAllowedException extends ApplicationException {

  public RoleRevocationNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, "No podés revocar roles.");
  }
}
