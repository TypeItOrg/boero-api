package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record EnrollmentApplicationResponse(
    UUID applicationId,
    UUID institutionId,
    UUID personId,
    String applicantFirstName,
    String applicantLastName,
    String applicantDocumentNumber,
    UUID studyPlanId,
    String studyPlanName,
    UUID academicYearId,
    Integer academicYear,
    UUID enrollmentPeriodId,
    EnrollmentApplicationStatus status,
    boolean isEditable,
    @Schema(nullable = true) String secondarySchool,
    @Schema(nullable = true) String rejectionReason,
    @Schema(nullable = true) LocalDateTime resolvedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return EnrollmentApplicationResponse.builder()
        .applicationId(application.getId())
        .institutionId(application.getInstitution().getId())
        .personId(application.getApplicantPerson().getId())
        .applicantFirstName(application.getApplicantPerson().getFirstName())
        .applicantLastName(application.getApplicantPerson().getLastName())
        .applicantDocumentNumber(application.getApplicantPerson().getDocumentNumber())
        .studyPlanId(application.getStudyPlan().getId())
        .studyPlanName(application.getStudyPlan().getName())
        .academicYearId(application.getAcademicYear().getId())
        .academicYear(application.getAcademicYear().getYear())
        .enrollmentPeriodId(application.getEnrollmentPeriod().getId())
        .status(application.getStatus())
        .isEditable(application.isEditable())
        .secondarySchool(
            application.getEducationBackground() != null
                ? application.getEducationBackground().getSecondarySchool()
                : null)
        .rejectionReason(application.getRejectionReason())
        .resolvedAt(application.getResolvedAt())
        .createdAt(application.getCreatedAt())
        .updatedAt(application.getUpdatedAt())
        .build();
  }
}
