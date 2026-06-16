package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.LAST_INSTITUTIONAL_AUTHORITY_REVOCATION;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LastInstitutionalAuthorityRevocationException extends ResponseStatusException {
  public LastInstitutionalAuthorityRevocationException() {
    super(HttpStatus.CONFLICT, LAST_INSTITUTIONAL_AUTHORITY_REVOCATION);
  }
}
