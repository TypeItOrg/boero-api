package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {
      "fullName",
      "documentNumber",
      "occupation",
      "phoneNumber",
      "email",
      "educationLevel"
    })
public record ResponsibleDto(
    @Schema(nullable = true) String fullName,
    @Schema(nullable = true) String documentNumber,
    @Schema(nullable = true) String occupation,
    @Schema(nullable = true) String phoneNumber,
    @Schema(nullable = true) String email,
    @Schema(nullable = true) String educationLevel) {
  public ResponsibleDto() {
    this(null, null, null, null, null, null);
  }

  public String getFullName() {
    return fullName;
  }

  public String getDocumentNumber() {
    return documentNumber;
  }

  public String getOccupation() {
    return occupation;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getEmail() {
    return email;
  }

  public String getEducationLevel() {
    return educationLevel;
  }
}
