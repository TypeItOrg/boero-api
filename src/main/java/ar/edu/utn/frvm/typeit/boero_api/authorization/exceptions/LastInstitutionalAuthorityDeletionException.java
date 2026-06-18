package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.LAST_INSTITUTIONAL_AUTHORITY_DELETION;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LastInstitutionalAuthorityDeletionException extends ResponseStatusException {
  public LastInstitutionalAuthorityDeletionException() {
    super(HttpStatus.CONFLICT, LAST_INSTITUTIONAL_AUTHORITY_DELETION);
  }
}
