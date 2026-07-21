package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.platformPrincipal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  @Mock private AuthorityResolver authorityResolver;

  @InjectMocks private AuthorizationService authorizationService;

  @Test
  @DisplayName("Should allow access when institutional principal has the required permission")
  void hasPermission_returnsTrueWhenGranted() {
    UUID userId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    JwtAuthenticatedUser principal = institutionalPrincipal(userId, institutionId);
    var authentication = new UsernamePasswordAuthenticationToken(principal, null);

    when(authorityResolver.resolveForPerson(principal.personId(), institutionId))
        .thenReturn(Set.of(PermissionCode.INSTITUTION_ROLE_ASSIGN));

    assertThat(
            authorizationService.hasPermission(
                authentication, PermissionCode.INSTITUTION_ROLE_ASSIGN))
        .isTrue();
    assertThat(
            authorizationService.hasPermission(
                authentication, PermissionCode.INSTITUTION_ROLE_REVOKE))
        .isFalse();
  }

  @Test
  @DisplayName("Should allow access when platform principal has the required role")
  void hasPlatformRole_returnsTrueWhenGranted() {
    UUID platformAccountId = UUID.randomUUID();
    var principal = platformPrincipal(platformAccountId);
    var authentication = new UsernamePasswordAuthenticationToken(principal, null);

    when(authorityResolver.resolvePlatformRoles(platformAccountId))
        .thenReturn(Set.of(PlatformRoleCode.PLATFORM_ADMIN));

    assertThat(
            authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN))
        .isTrue();
  }

  @Test
  @DisplayName("Should deny platform role checks for institutional principals")
  void hasPlatformRole_returnsFalseForInstitutionalPrincipal() {
    var principal = institutionalPrincipal(UUID.randomUUID(), UUID.randomUUID());
    var authentication = new UsernamePasswordAuthenticationToken(principal, null);

    assertThat(
            authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN))
        .isFalse();
  }
}
