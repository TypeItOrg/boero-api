package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PlatformPersonSummaryResponse(
    UUID id,
    String firstName,
    String lastName,
    String documentNumber,
    String email,
    @Nullable String phoneNumber,
    UUID institutionId,
    String institutionName,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
    List<PersonSummaryResponse.PersonRoleSummaryResponse> roles) {

  public static PlatformPersonSummaryResponse from(
      final Person person,
      final boolean enabled,
      final List<PersonSummaryResponse.PersonRoleSummaryResponse> roles) {
    return new PlatformPersonSummaryResponse(
        person.getId(),
        person.getFirstName(),
        person.getLastName(),
        person.getDocumentNumber(),
        person.getEmail(),
        person.getPhoneNumber(),
        person.getInstitution().getId(),
        person.getInstitution().getName(),
        enabled,
        roles);
  }
}
