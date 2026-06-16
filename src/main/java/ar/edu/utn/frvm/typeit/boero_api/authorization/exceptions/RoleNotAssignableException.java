package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_NOT_ASSIGNABLE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RoleNotAssignableException extends ResponseStatusException {
  public RoleNotAssignableException() {
    super(HttpStatus.BAD_REQUEST, ROLE_NOT_ASSIGNABLE);
  }
}
