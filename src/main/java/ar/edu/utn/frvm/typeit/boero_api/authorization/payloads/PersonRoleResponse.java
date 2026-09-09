package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.time.Instant;
import lombok.Builder;

@Builder
public record PersonRoleResponse(
    java.util.UUID roleId, SystemRoleCode technicalCode, String displayName, Instant assignedAt) {

  public static PersonRoleResponse from(PersonRoleAssignment assignment) {
    SystemRoleCode technicalCode =
        assignment.getRole().isSystem()
            ? SystemRoleCode.valueOf(assignment.getRole().getCode())
            : null;
    return PersonRoleResponse.builder()
        .roleId(assignment.getRole().getId())
        .technicalCode(technicalCode)
        .displayName(assignment.getRole().getName())
        .assignedAt(assignment.getCreatedAt())
        .build();
  }
}
