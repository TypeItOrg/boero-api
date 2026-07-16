package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.platformPrincipal;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

class InstitutionalCallerGuardTest {

  private static final UUID USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_INSTITUTION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID PLATFORM_ACCOUNT_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  private final InstitutionalCallerGuard institutionalCallerGuard = new InstitutionalCallerGuard();

  @Test
  @DisplayName("Should reject platform admin on institution guard")
  void ensureCallerBelongsToInstitution_rejectsPlatformAdmin() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    assertThatThrownBy(
            () ->
                institutionalCallerGuard.ensureCallerBelongsToInstitution(
                    authentication, INSTITUTION_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject platform account without platform admin role")
  void ensureCallerBelongsToInstitution_rejectsNonAdminPlatformAccount() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    assertThatThrownBy(
            () ->
                institutionalCallerGuard.ensureCallerBelongsToInstitution(
                    authentication, INSTITUTION_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should allow institutional user from same institution")
  void ensureCallerBelongsToInstitution_allowsSameInstitution() {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), null);

    assertThatCode(
            () ->
                institutionalCallerGuard.ensureCallerBelongsToInstitution(
                    authentication, INSTITUTION_ID))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject institutional user from different institution")
  void ensureCallerBelongsToInstitution_rejectsDifferentInstitution() {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), null);

    assertThatThrownBy(
            () ->
                institutionalCallerGuard.ensureCallerBelongsToInstitution(
                    authentication, OTHER_INSTITUTION_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject platform admin on institutional principal guard")
  void ensureInstitutionalPrincipal_rejectsPlatformAdmin() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    assertThatThrownBy(() -> institutionalCallerGuard.ensureInstitutionalPrincipal(authentication))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject non-admin platform account on institutional principal guard")
  void ensureInstitutionalPrincipal_rejectsNonAdminPlatformAccount() {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    assertThatThrownBy(() -> institutionalCallerGuard.ensureInstitutionalPrincipal(authentication))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should allow institutional user on institutional principal guard")
  void ensureInstitutionalPrincipal_allowsInstitutionalUser() {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), null);

    assertThatCode(() -> institutionalCallerGuard.ensureInstitutionalPrincipal(authentication))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject unauthenticated request on institutional principal guard")
  void ensureInstitutionalPrincipal_rejectsUnauthenticated() {
    assertThatThrownBy(() -> institutionalCallerGuard.ensureInstitutionalPrincipal(null))
        .isInstanceOf(AccessDeniedException.class);
  }
}
