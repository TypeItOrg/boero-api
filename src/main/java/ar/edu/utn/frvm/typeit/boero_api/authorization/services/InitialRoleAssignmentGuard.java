package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialRoleAssignmentGuard {

  private final AuthorizationService authorizationService;

  public void check(final Authentication authentication, final SystemRoleCode initialRole) {
    if (initialRole == null || initialRole == SystemRoleCode.APPLICANT) {
      return;
    }

    final boolean platformAdmin =
        authentication != null
            && authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount
            && authorizationService.hasPlatformRole(
                authentication, PlatformRoleCode.PLATFORM_ADMIN);
    if (platformAdmin) {
      return;
    }

    final boolean canAssignRoles =
        authorizationService.hasPermission(authentication, PermissionCode.INSTITUTION_ROLE_ASSIGN);
    if (!canAssignRoles) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }
}
