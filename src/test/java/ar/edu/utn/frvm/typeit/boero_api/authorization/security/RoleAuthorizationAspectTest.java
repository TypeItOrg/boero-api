package ar.edu.utn.frvm.typeit.boero_api.authorization.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPlatformRole;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationAspectTest {

  @Mock private AuthorizationService authorizationService;

  @InjectMocks private RoleAuthorizationAspect roleAuthorizationAspect;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should deny access when platform role is not granted")
  void checkPlatformRole_deniesWhenRoleMissing() {
    var authentication = new TestingAuthenticationToken("anonymous", null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN))
        .thenReturn(false);

    assertThatThrownBy(() -> roleAuthorizationAspect.checkPlatformRole(platformAdminRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should allow access when platform role is granted")
  void checkPlatformRole_allowsWhenRoleGranted() {
    var authentication = new TestingAuthenticationToken("admin@plataforma.com", null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN))
        .thenReturn(true);

    assertThatCode(() -> roleAuthorizationAspect.checkPlatformRole(platformAdminRequired()))
        .doesNotThrowAnyException();
  }

  private static RequiresPlatformRole platformAdminRequired() {
    RequiresPlatformRole annotation = mock(RequiresPlatformRole.class);
    when(annotation.value()).thenReturn(PlatformRoleCode.PLATFORM_ADMIN);
    return annotation;
  }
}
