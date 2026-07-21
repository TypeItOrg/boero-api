package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record UserPayload(
    UUID userId,
    UUID personId,
    String name,
    String lastName,
    String documentNumber,
    UUID institutionId,
    List<String> roles,
    Set<String> permissions) {

  public UserPayload {
    roles = roles == null ? List.of() : List.copyOf(roles);
    permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
  }

  public static UserPayload from(User user, UUID personId, Set<PermissionCode> permissions) {
    return from(user, personId, permissions, List.of());
  }

  public static UserPayload from(
      User user, UUID personId, Set<PermissionCode> permissions, List<String> roles) {
    Set<PermissionCode> grantedPermissions = permissions == null ? Set.of() : permissions;

    return UserPayload.builder()
        .userId(user.getId())
        .personId(personId)
        .name(user.getName())
        .lastName(user.getLastName())
        .documentNumber(user.getDocumentNumber())
        .institutionId(user.getInstitutionId())
        .roles(roles)
        .permissions(
            grantedPermissions.stream()
                .map(PermissionCode::getCode)
                .collect(Collectors.toUnmodifiableSet()))
        .build();
  }
}
