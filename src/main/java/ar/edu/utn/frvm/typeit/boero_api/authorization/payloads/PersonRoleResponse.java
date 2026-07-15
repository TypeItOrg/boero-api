package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Builder;

@Builder
public record PersonRoleResponse(
    SystemRoleCode roleCode, String displayName, OffsetDateTime assignedAt) {

  public static PersonRoleResponse from(PersonRoleAssignment assignment) {
    SystemRoleCode roleCode = SystemRoleCode.valueOf(assignment.getRole().getCode());
    return PersonRoleResponse.builder()
        .roleCode(roleCode)
        .displayName(assignment.getRole().getName())
        .assignedAt(toUtcOffset(assignment.getCreatedAt()))
        .build();
  }

  private static OffsetDateTime toUtcOffset(final LocalDateTime value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }
}
