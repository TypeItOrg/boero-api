package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionResponse(
    UUID id,
    String name,
    String slug,
    String city,
    String province,
    String phoneNumber,
    String email,
    boolean active) {

  public static InstitutionResponse from(Institution institution) {
    return InstitutionResponse.builder()
        .id(institution.getId())
        .name(institution.getName())
        .slug(institution.getSlug())
        .city(institution.getCity().getName())
        .province(institution.getCity().getProvince().getName())
        .phoneNumber(institution.getPhoneNumber())
        .email(institution.getEmail())
        .active(institution.isActive())
        .build();
  }
}
