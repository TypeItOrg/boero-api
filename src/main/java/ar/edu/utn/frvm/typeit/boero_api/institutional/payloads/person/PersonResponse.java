package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CountrySummaryResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PersonResponse(
    UUID personId,
    String firstName,
    String lastName,
    String documentNumber,
    LocalDate birthDate,
    String phoneNumber,
    String email,
    UUID institutionId,
    String institutionName,
    AddressResponse address,
    CitySummaryResponse birthCity,
    CountrySummaryResponse nationalityCountry) {

  public static PersonResponse from(Person person) {
    return PersonResponse.builder()
        .personId(person.getId())
        .firstName(person.getFirstName())
        .lastName(person.getLastName())
        .documentNumber(person.getDocumentNumber())
        .birthDate(person.getBirthDate())
        .phoneNumber(person.getPhoneNumber())
        .email(person.getEmail())
        .institutionId(person.getInstitution().getId())
        .institutionName(person.getInstitution().getName())
        .address(person.getAddress() != null ? AddressResponse.from(person.getAddress()) : null)
        .birthCity(
            person.getBirthCity() != null
                ? CitySummaryResponse.builder()
                    .id(person.getBirthCity().getId())
                    .name(person.getBirthCity().getName())
                    .provinceId(person.getBirthCity().getProvince().getId())
                    .province(person.getBirthCity().getProvince().getName())
                    .build()
                : null)
        .nationalityCountry(
            person.getNationalityCountry() != null
                ? CountrySummaryResponse.from(person.getNationalityCountry())
                : null)
        .build();
  }
}
