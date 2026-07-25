package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record ReplacePersonRolesRequest(@NotEmpty Set<@NotNull UUID> roleIds) {

  public ReplacePersonRolesRequest {
    roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
  }
}
