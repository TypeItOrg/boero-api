package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        toResponseJson(application),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }

  private static JsonNode toResponseJson(final EnrollmentApplication application) {
    return OBJECT_MAPPER.readTree(application.getData().toString());
  }
}
