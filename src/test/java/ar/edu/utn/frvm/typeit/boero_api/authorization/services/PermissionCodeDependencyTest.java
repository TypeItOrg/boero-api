package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionCodeDependencyTest {

  @Test
  @DisplayName("Should include the people read permission for people actions")
  void withRequiredPermissions_includesPeopleReadPermission() {
    Set<PermissionCode> expanded =
        PermissionCode.withRequiredPermissions(Set.of(PermissionCode.INSTITUTION_PERSON_DELETE));

    assertThat(expanded)
        .containsExactlyInAnyOrder(
            PermissionCode.INSTITUTION_PERSON_DELETE, PermissionCode.INSTITUTION_PERSON_READ_ANY);
  }

  @Test
  @DisplayName("Should include both module bases for role assignment")
  void withRequiredPermissions_includesBothModuleBases() {
    Set<PermissionCode> expanded =
        PermissionCode.withRequiredPermissions(Set.of(PermissionCode.INSTITUTION_ROLE_ASSIGN));

    assertThat(expanded)
        .containsExactlyInAnyOrder(
            PermissionCode.INSTITUTION_ROLE_ASSIGN,
            PermissionCode.INSTITUTION_PERSON_READ_ANY,
            PermissionCode.INSTITUTION_ROLE_READ);
  }
}
