package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.LAST_PERSON_ROLE_REVOCATION;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LastPersonRoleRevocationException extends ResponseStatusException {
  public LastPersonRoleRevocationException() {
    super(HttpStatus.CONFLICT, LAST_PERSON_ROLE_REVOCATION);
  }
}
