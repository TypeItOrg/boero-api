package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
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
    Set<String> permissions) {

  public UserPayload {
    permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
  }

  public static UserPayload from(User user, UUID personId, Set<PermissionCode> permissions) {
    Set<PermissionCode> grantedPermissions = permissions == null ? Set.of() : permissions;

    return UserPayload.builder()
        .userId(user.getId())
        .personId(personId)
        .name(user.getName())
        .lastName(user.getLastName())
        .documentNumber(user.getDocumentNumber())
        .institutionId(user.getInstitutionId())
        .permissions(
            grantedPermissions.stream()
                .map(PermissionCode::getCode)
                .collect(Collectors.toUnmodifiableSet()))
        .build();
  }
}
