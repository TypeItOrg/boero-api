package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

public record InstitutionalAuthoritySnapshot(Set<PermissionCode> permissions, List<String> roles)
    implements Serializable {

  public InstitutionalAuthoritySnapshot {
    permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    roles = roles == null ? List.of() : List.copyOf(roles);
  }
}
