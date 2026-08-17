package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class AcademicLifecycleActorResolver {

  AcademicLifecycleActor resolve() {
    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new AccessDeniedException("No fue posible identificar al actor de la operación.");
    }
    if (authentication.getPrincipal() instanceof JwtAuthenticatedUser user) {
      return new AcademicLifecycleActor(user.accountType(), user.userId());
    }
    if (authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount platform) {
      return new AcademicLifecycleActor(platform.accountType(), platform.platformAccountId());
    }
    throw new AccessDeniedException("No fue posible identificar al actor de la operación.");
  }
}
