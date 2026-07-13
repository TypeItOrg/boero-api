package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.List;
import java.util.UUID;

public record PersonSummaryResponse(
    UUID id,
    String firstName,
    String lastName,
    String documentNumber,
    String email,
    String phoneNumber,
    List<PersonRoleSummaryResponse> roles) {

  public record PersonRoleSummaryResponse(String roleCode, String displayName) {}

  public static PersonSummaryResponse from(final Person person) {
    return from(person, List.of());
  }

  public static PersonSummaryResponse from(
      final Person person, final List<PersonRoleSummaryResponse> roles) {
    return new PersonSummaryResponse(
        person.getId(),
        person.getFirstName(),
        person.getLastName(),
        person.getDocumentNumber(),
        person.getEmail(),
        person.getPhoneNumber(),
        roles);
  }
}
