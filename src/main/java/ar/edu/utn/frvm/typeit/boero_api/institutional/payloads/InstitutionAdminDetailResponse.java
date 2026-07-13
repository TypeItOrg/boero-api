package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstitutionAdminDetailResponse(
    UUID id,
    String name,
    String slug,
    CitySummaryResponse city,
    ProvinceSummaryResponse province,
    CountryLocationResponse country,
    String street,
    String number,
    String neighborhood,
    String additionalInfo,
    String phoneNumber,
    String email,
    boolean active,
    long userCount) {

  public static InstitutionAdminDetailResponse from(Institution institution, long userCount) {
    var city = institution.getCity();
    var province = city.getProvince();
    var country = province.getCountry();

    return InstitutionAdminDetailResponse.builder()
        .id(institution.getId())
        .name(institution.getName())
        .slug(institution.getSlug())
        .city(CitySummaryResponse.from(city))
        .province(ProvinceSummaryResponse.from(province))
        .country(CountryLocationResponse.from(country))
        .street(institution.getStreet())
        .number(institution.getNumber())
        .neighborhood(institution.getNeighborhood())
        .additionalInfo(institution.getAdditionalInfo())
        .phoneNumber(institution.getPhoneNumber())
        .email(institution.getEmail())
        .active(institution.isActive())
        .userCount(userCount)
        .build();
  }
}
