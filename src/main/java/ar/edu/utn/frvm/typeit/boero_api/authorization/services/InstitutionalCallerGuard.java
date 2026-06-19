package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstitutionalCallerGuard {

  private final AuthorizationService authorizationService;

  public void ensureInstitutionalPrincipal(Authentication authentication) {
    if (isPlatformAdmin(authentication)) return;

    if (authentication == null
        || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }

  public void ensureCallerBelongsToInstitution(Authentication authentication, UUID institutionId) {
    if (isPlatformAdmin(authentication)) return;

    if (authentication == null
        || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser user)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }

    if (!user.institutionId().equals(institutionId)) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }

  private boolean isPlatformAdmin(Authentication authentication) {
    return authentication != null
        && authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount
        && authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN);
  }
}
