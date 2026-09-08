package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "academicYearId",
      "academicYearNumber",
      "name",
      "startDate",
      "endDate",
      "status"
    })
public record EnrollmentPeriodResponse(
    UUID id,
    UUID institutionId,
    UUID academicYearId,
    int academicYearNumber,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    EnrollmentPeriodStatus status,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static EnrollmentPeriodResponse from(final EnrollmentPeriod period) {
    return new EnrollmentPeriodResponse(
        period.getId(),
        period.getInstitution().getId(),
        period.getAcademicYear().getId(),
        period.getAcademicYear().getYear(),
        period.getName(),
        period.getStartDate(),
        period.getEndDate(),
        period.getStatus(),
        period.getDeletedAt());
  }
}
