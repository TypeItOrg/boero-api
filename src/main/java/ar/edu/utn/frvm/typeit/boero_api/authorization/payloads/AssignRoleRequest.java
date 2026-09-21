package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.AccessScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.InvalidAccessScopeException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Schema(requiredProperties = {"roleId", "accessScope", "trainingPathIds"})
public record AssignRoleRequest(
    @NotNull UUID roleId,
    @NotNull AccessScope accessScope,
    @NotNull List<@NotNull UUID> trainingPathIds) {
  public AssignRoleRequest {
    if (trainingPathIds != null) {
      if (new HashSet<>(trainingPathIds).size() != trainingPathIds.size()
          || trainingPathIds.stream().anyMatch(Objects::isNull)) {
        throw new InvalidAccessScopeException();
      }
      trainingPathIds = List.copyOf(trainingPathIds);
    }
  }

  public AssignRoleRequest(UUID roleId, AccessScope accessScope, Set<UUID> trainingPathIds) {
    this(roleId, accessScope, trainingPathIds == null ? null : List.copyOf(trainingPathIds));
  }

  public Set<UUID> selectedTrainingPathIds() {
    return trainingPathIds == null ? null : Set.copyOf(trainingPathIds);
  }
}
