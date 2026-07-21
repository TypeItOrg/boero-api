package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PlatformRoleListItemResponse(
    UUID id,
    String name,
    SystemRoleCode technicalCode,
    boolean editable,
    boolean deletable,
    long assignmentCount,
    int permissionCount,
    PlatformRoleResponse.PlatformRoleInstitution institution) {

  public static PlatformRoleListItemResponse from(
      Role role, long assignmentCount, int permissionCount) {
    SystemRoleCode technicalCode = role.isSystem() ? SystemRoleCode.valueOf(role.getCode()) : null;
    boolean authority = technicalCode == SystemRoleCode.INSTITUTIONAL_AUTHORITY;
    boolean institutionActive = role.getInstitution().isActive();
    return PlatformRoleListItemResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .technicalCode(technicalCode)
        .editable(!authority && institutionActive)
        .deletable(!role.isSystem() && assignmentCount == 0 && institutionActive)
        .assignmentCount(assignmentCount)
        .permissionCount(permissionCount)
        .institution(PlatformRoleResponse.PlatformRoleInstitution.fromRole(role))
        .build();
  }
}
