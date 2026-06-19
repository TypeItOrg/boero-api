package ar.edu.utn.frvm.typeit.boero_api.authorization.security;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.security.AuthorizationAspectSupport.denyUnless;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresAnyPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationAspect {

  private final AuthorizationService authorizationService;

  @Before("@annotation(requiresPermission)")
  public void checkPermission(RequiresPermission requiresPermission) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isPlatformAdmin(authentication)) return;

    denyUnless(authorizationService.hasPermission(authentication, requiresPermission.value()));
  }

  @Before("@annotation(requiresAnyPermission)")
  public void checkAnyPermission(RequiresAnyPermission requiresAnyPermission) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isPlatformAdmin(authentication)) return;

    denyUnless(
        authorizationService.hasAnyPermission(authentication, requiresAnyPermission.value()));
  }

  private boolean isPlatformAdmin(Authentication authentication) {
    return authentication != null
        && authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount
        && authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN);
  }
}
