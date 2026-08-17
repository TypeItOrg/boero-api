package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "year",
      "startDate",
      "endDate",
      "status",
      "deletedAt"
    })
public record AcademicYearResponse(
    UUID id,
    UUID institutionId,
    int year,
    @Schema(nullable = true) LocalDate startDate,
    @Schema(nullable = true) LocalDate endDate,
    AcademicYearStatus status,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static AcademicYearResponse from(final AcademicYear year) {
    return new AcademicYearResponse(
        year.getId(),
        year.getInstitution().getId(),
        year.getYear(),
        year.getStartDate(),
        year.getEndDate(),
        year.getStatus(),
        year.getDeletedAt());
  }
}
