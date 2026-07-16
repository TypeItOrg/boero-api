package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalCallerGuard {

  public void ensureInstitutionalPrincipal(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }

  public void ensureCallerBelongsToInstitution(Authentication authentication, UUID institutionId) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser user)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }

    if (!user.institutionId().equals(institutionId)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }
}
