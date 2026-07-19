package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionRoleResponse(
    UUID id,
    String name,
    SystemRoleCode technicalCode,
    boolean editable,
    boolean deletable,
    long assignmentCount,
    Set<String> permissions,
    Set<String> protectedPermissions) {

  public static InstitutionRoleResponse from(
      Role role, long assignmentCount, Set<String> permissions) {
    return from(role, assignmentCount, permissions, Set.of());
  }

  public static InstitutionRoleResponse from(
      Role role, long assignmentCount, Set<String> permissions, Set<String> protectedPermissions) {
    SystemRoleCode technicalCode = role.isSystem() ? SystemRoleCode.valueOf(role.getCode()) : null;
    boolean authority = technicalCode == SystemRoleCode.INSTITUTIONAL_AUTHORITY;
    return InstitutionRoleResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .technicalCode(technicalCode)
        .editable(!authority)
        .deletable(!role.isSystem() && assignmentCount == 0)
        .assignmentCount(assignmentCount)
        .permissions(permissions)
        .protectedPermissions(protectedPermissions)
        .build();
  }
}
