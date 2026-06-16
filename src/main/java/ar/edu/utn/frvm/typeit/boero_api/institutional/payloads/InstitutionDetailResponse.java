package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionDetailResponse(
    UUID id,
    String name,
    String slug,
    String city,
    String province,
    String street,
    String number,
    String neighborhood,
    String additionalInfo,
    String phoneNumber,
    String email,
    boolean active) {

  public static InstitutionDetailResponse from(Institution institution) {
    return InstitutionDetailResponse.builder()
        .id(institution.getId())
        .name(institution.getName())
        .slug(institution.getSlug())
        .city(institution.getCity().getName())
        .province(institution.getCity().getProvince().getName())
        .street(institution.getStreet())
        .number(institution.getNumber())
        .neighborhood(institution.getNeighborhood())
        .additionalInfo(institution.getAdditionalInfo())
        .phoneNumber(institution.getPhoneNumber())
        .email(institution.getEmail())
        .active(institution.isActive())
        .build();
  }
}
