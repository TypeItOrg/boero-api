package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record EnrollmentApplicationResponse(
    UUID applicationId,
    UUID personId,
    UUID institutionId,
    UUID studyPlanId,
    UUID academicYearId,
    UUID enrollmentPeriodId,
    EnrollmentApplicationStatus status,
    boolean isEditable,
    JsonNode data,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return new EnrollmentApplicationResponse(
        application.getId(),
        application.getPerson().getId(),
        application.getInstitution().getId(),
        application.getStudyPlan().getId(),
        application.getAcademicYear().getId(),
        application.getEnrollmentPeriodId(),
        application.getStatus(),
        application.isEditable(),
        application.getData(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}
