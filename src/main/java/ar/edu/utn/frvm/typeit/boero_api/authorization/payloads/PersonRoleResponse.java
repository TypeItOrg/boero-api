package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.AccessScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder
@io.swagger.v3.oas.annotations.media.Schema(
    requiredProperties = {
      "roleId",
      "technicalCode",
      "displayName",
      "assignedAt",
      "accessScope",
      "trainingPathIds",
      "trainingPathNames"
    })
public record PersonRoleResponse(
    java.util.UUID roleId,
    @io.swagger.v3.oas.annotations.media.Schema(nullable = true) SystemRoleCode technicalCode,
    String displayName,
    Instant assignedAt,
    AccessScope accessScope,
    Set<java.util.UUID> trainingPathIds,
    Map<java.util.UUID, String> trainingPathNames) {

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
        .accessScope(assignment.getAccessScope())
        .trainingPathIds(Set.copyOf(assignment.getTrainingPathIds()))
        .trainingPathNames(Map.of())
        .build();
  }
}
