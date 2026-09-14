package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {
      "firstName",
      "lastName",
      "documentNumber",
      "birthDate",
      "phoneNumber",
      "email"
    })
public record PersonalDataDto(
    @Schema(nullable = true) String firstName,
    @Schema(nullable = true) String lastName,
    @Schema(nullable = true) String documentNumber,
    @Schema(nullable = true) LocalDate birthDate,
    @Schema(nullable = true) String phoneNumber,
    @Schema(nullable = true) String email) {
  public PersonalDataDto() {
    this(null, null, null, null, null, null);
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getDocumentNumber() {
    return documentNumber;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getEmail() {
    return email;
  }
}
