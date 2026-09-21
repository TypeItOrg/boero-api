package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.AccessScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRoleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScopedAuthoritySnapshotTest {
  private final PersonRoleAssignmentRepository assignments =
      mock(PersonRoleAssignmentRepository.class);
  private final CachedAuthoritySnapshotResolver resolver =
      new CachedAuthoritySnapshotResolver(assignments, mock(PlatformAccountRoleRepository.class));

  @Test
  void preservesPermissionScopeAndDisablesInstitutionalPermissionsInLimitedRoles() {
    UUID person = UUID.randomUUID(),
        institution = UUID.randomUUID(),
        professor = UUID.randomUUID(),
        technical = UUID.randomUUID();
    when(assignments.findAuthoritiesByPersonIdAndInstitutionId(person, institution))
        .thenReturn(
            List.of(
                row(
                    PermissionCode.ENROLLMENT_APPLICATION_READ,
                    AccessScope.TRAINING_PATHS,
                    professor),
                row(
                    PermissionCode.ENROLLMENT_APPLICATION_APPROVE,
                    AccessScope.TRAINING_PATHS,
                    professor),
                row(
                    PermissionCode.ENROLLMENT_APPLICATION_READ,
                    AccessScope.TRAINING_PATHS,
                    technical),
                row(
                    PermissionCode.INSTITUTION_PERSON_UPDATE_ANY,
                    AccessScope.TRAINING_PATHS,
                    professor)));
    var snapshot = resolver.resolveFreshForPerson(person, institution);
    assertThat(
            snapshot
                .permissionScopes()
                .get(PermissionCode.ENROLLMENT_APPLICATION_READ)
                .trainingPathIds())
        .containsExactlyInAnyOrder(professor, technical);
    assertThat(
            snapshot
                .permissionScopes()
                .get(PermissionCode.ENROLLMENT_APPLICATION_APPROVE)
                .trainingPathIds())
        .containsExactly(professor);
    assertThat(snapshot.permissions()).doesNotContain(PermissionCode.INSTITUTION_PERSON_UPDATE_ANY);
  }

  @Test
  void institutionalGrantIncludesFuturePathsWithoutWideningOtherPermissions() {
    UUID person = UUID.randomUUID(), institution = UUID.randomUUID(), path = UUID.randomUUID();
    when(assignments.findAuthoritiesByPersonIdAndInstitutionId(person, institution))
        .thenReturn(
            List.of(
                row(PermissionCode.ENROLLMENT_APPLICATION_READ, AccessScope.INSTITUTION, null),
                row(
                    PermissionCode.ENROLLMENT_APPLICATION_APPROVE,
                    AccessScope.TRAINING_PATHS,
                    path)));
    var snapshot = resolver.resolveFreshForPerson(person, institution);
    assertThat(
            snapshot
                .permissionScopes()
                .get(PermissionCode.ENROLLMENT_APPLICATION_READ)
                .includes(UUID.randomUUID()))
        .isTrue();
    assertThat(
            snapshot
                .permissionScopes()
                .get(PermissionCode.ENROLLMENT_APPLICATION_APPROVE)
                .includes(UUID.randomUUID()))
        .isFalse();
  }

  private PersonRoleAssignmentRepository.AuthorityRow row(
      PermissionCode permission, AccessScope scope, UUID path) {
    return new PersonRoleAssignmentRepository.AuthorityRow() {
      public String getRoleName() {
        return "Preceptor";
      }

      public String getPermissionCode() {
        return permission.getCode();
      }

      public AccessScope getAccessScope() {
        return scope;
      }

      public UUID getTrainingPathId() {
        return path;
      }
    };
  }
}
