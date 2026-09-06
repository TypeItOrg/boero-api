package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentApplicationResponse {

  private UUID applicationId;
  private UUID institutionId;
  private UUID personId;
  private String applicantFirstName;
  private String applicantLastName;
  private String applicantDocumentNumber;
  private UUID studyPlanId;
  private String studyPlanName;
  private UUID academicYearId;
  private Integer academicYear;
  private UUID enrollmentPeriodId;
  private EnrollmentApplicationStatus status;
  private boolean isEditable;
  private EnrollmentDraftData data;
  @Schema(nullable = true) private String secondarySchool;
  @Schema(nullable = true) private String rejectionReason;
  @Schema(nullable = true) private LocalDateTime resolvedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UUID applicationId() {
    return applicationId;
  }

  public EnrollmentApplicationStatus status() {
    return status;
  }

  public String rejectionReason() {
    return rejectionReason;
  }

  public LocalDateTime resolvedAt() {
    return resolvedAt;
  }

  public static EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return EnrollmentApplicationResponse.builder()
        .applicationId(application.getId())
        .institutionId(
            application.getInstitution() != null ? application.getInstitution().getId() : null)
        .personId(
            application.getApplicantPerson() != null
                ? application.getApplicantPerson().getId()
                : null)
        .applicantFirstName(
            application.getApplicantPerson() != null
                ? application.getApplicantPerson().getFirstName()
                : null)
        .applicantLastName(
            application.getApplicantPerson() != null
                ? application.getApplicantPerson().getLastName()
                : null)
        .applicantDocumentNumber(
            application.getApplicantPerson() != null
                ? application.getApplicantPerson().getDocumentNumber()
                : null)
        .studyPlanId(
            application.getStudyPlan() != null ? application.getStudyPlan().getId() : null)
        .studyPlanName(
            application.getStudyPlan() != null ? application.getStudyPlan().getName() : null)
        .academicYearId(
            application.getAcademicYear() != null ? application.getAcademicYear().getId() : null)
        .academicYear(
            application.getAcademicYear() != null ? application.getAcademicYear().getYear() : null)
        .enrollmentPeriodId(
            application.getEnrollmentPeriod() != null
                ? application.getEnrollmentPeriod().getId()
                : null)
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
