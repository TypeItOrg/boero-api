package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionListItemResponse(
    UUID id, String name, String slug, String city, String province) {

  public static InstitutionListItemResponse from(Institution institution) {
    return InstitutionListItemResponse.builder()
        .id(institution.getId())
        .name(institution.getName())
        .slug(institution.getSlug())
        .city(institution.getCity().getName())
        .province(institution.getCity().getProvince().getName())
        .build();
  }
}
