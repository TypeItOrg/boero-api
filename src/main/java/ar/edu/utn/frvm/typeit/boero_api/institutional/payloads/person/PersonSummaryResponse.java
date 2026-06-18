package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.UUID;

public record PersonSummaryResponse(
    UUID id, String firstName, String lastName, String documentNumber, String email) {

  public static PersonSummaryResponse from(final Person person) {
    return new PersonSummaryResponse(
        person.getId(),
        person.getFirstName(),
        person.getLastName(),
        person.getDocumentNumber(),
        person.getEmail());
  }
}
