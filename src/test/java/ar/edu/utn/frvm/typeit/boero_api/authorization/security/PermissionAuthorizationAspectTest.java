package ar.edu.utn.frvm.typeit.boero_api.authorization.security;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.platformPrincipal;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresAnyPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import java.util.UUID;
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
class PermissionAuthorizationAspectTest {

  private static final UUID PLATFORM_ACCOUNT_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Mock private AuthorizationService authorizationService;

  @InjectMocks private PermissionAuthorizationAspect permissionAuthorizationAspect;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should reject platform admin on institutional permission checks")
  void checkPermission_rejectsPlatformAdmin() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(
            authentication, PermissionCode.INSTITUTION_PERSON_READ_ANY))
        .thenReturn(false);

    assertThatThrownBy(() -> permissionAuthorizationAspect.checkPermission(permissionRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject platform admin on institutional any-permission checks")
  void checkAnyPermission_rejectsPlatformAdmin() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasAnyPermission(
            authentication,
            PermissionCode.INSTITUTION_PERSON_READ_ANY,
            PermissionCode.INSTITUTION_ROLE_ASSIGN))
        .thenReturn(false);

    assertThatThrownBy(
            () -> permissionAuthorizationAspect.checkAnyPermission(anyPermissionRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject non-admin platform account for permission checks")
  void checkPermission_rejectsNonAdminPlatformAccount() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(
            authentication, PermissionCode.INSTITUTION_PERSON_READ_ANY))
        .thenReturn(false);

    assertThatThrownBy(() -> permissionAuthorizationAspect.checkPermission(permissionRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject non-admin platform account for any-permission checks")
  void checkAnyPermission_rejectsNonAdminPlatformAccount() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasAnyPermission(
            authentication,
            PermissionCode.INSTITUTION_PERSON_READ_ANY,
            PermissionCode.INSTITUTION_ROLE_ASSIGN))
        .thenReturn(false);

    assertThatThrownBy(
            () -> permissionAuthorizationAspect.checkAnyPermission(anyPermissionRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should still enforce permissions for non-platform principals")
  void checkPermission_enforcesForNonPlatform() {
    var authentication = new TestingAuthenticationToken("anonymous", null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(any(), any(PermissionCode.class))).thenReturn(false);

    assertThatThrownBy(() -> permissionAuthorizationAspect.checkPermission(permissionRequired()))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static RequiresPermission permissionRequired() {
    RequiresPermission annotation = mock(RequiresPermission.class);
    lenient().when(annotation.value()).thenReturn(PermissionCode.INSTITUTION_PERSON_READ_ANY);
    return annotation;
  }

  private static RequiresAnyPermission anyPermissionRequired() {
    RequiresAnyPermission annotation = mock(RequiresAnyPermission.class);
    lenient()
        .when(annotation.value())
        .thenReturn(
            new PermissionCode[] {
              PermissionCode.INSTITUTION_PERSON_READ_ANY, PermissionCode.INSTITUTION_ROLE_ASSIGN
            });
    return annotation;
  }
}
