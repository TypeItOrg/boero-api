package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserPayload(
    UUID userId,
    UUID personId,
    String name,
    String lastName,
    String documentNumber,
    UUID institutionId) {

  public static UserPayload from(User user) {
    return UserPayload.builder()
        .userId(user.getId())
        .name(user.getName())
        .lastName(user.getLastName())
        .documentNumber(user.getDocumentNumber())
        .institutionId(user.getInstitutionId())
        .build();
  }

  public static UserPayload from(User user, UUID personId) {
    return UserPayload.builder()
        .userId(user.getId())
        .personId(personId)
        .name(user.getName())
        .lastName(user.getLastName())
        .documentNumber(user.getDocumentNumber())
        .institutionId(user.getInstitutionId())
        .build();
  }
}
