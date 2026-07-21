package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PlatformRoleResponse(
    UUID id,
    String name,
    SystemRoleCode technicalCode,
    boolean editable,
    boolean deletable,
    long assignmentCount,
    Set<String> permissions,
    Set<String> protectedPermissions,
    PlatformRoleInstitution institution) {

  public static PlatformRoleResponse from(
      Role role, long assignmentCount, Set<String> permissions, Set<String> protectedPermissions) {
    SystemRoleCode technicalCode = role.isSystem() ? SystemRoleCode.valueOf(role.getCode()) : null;
    boolean authority = technicalCode == SystemRoleCode.INSTITUTIONAL_AUTHORITY;
    boolean institutionActive = role.getInstitution().isActive();
    return PlatformRoleResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .technicalCode(technicalCode)
        .editable(!authority && institutionActive)
        .deletable(!role.isSystem() && assignmentCount == 0 && institutionActive)
        .assignmentCount(assignmentCount)
        .permissions(permissions)
        .protectedPermissions(protectedPermissions)
        .institution(PlatformRoleInstitution.fromRole(role))
        .build();
  }

  public record PlatformRoleInstitution(UUID id, String name, boolean active) {
    public static PlatformRoleInstitution fromRole(Role role) {
      return new PlatformRoleInstitution(
          role.getInstitution().getId(),
          role.getInstitution().getName(),
          role.getInstitution().isActive());
    }
  }
}
