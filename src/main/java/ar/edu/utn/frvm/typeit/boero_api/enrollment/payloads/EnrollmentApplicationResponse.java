package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantHealthInclusion;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantResponsible;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
        .data(buildDraftData(application))
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

  private static EnrollmentDraftData buildDraftData(final EnrollmentApplication entity) {
    PersonalDataDto personalDataDto = null;
    Person applicant = entity.getApplicantPerson();
    if (applicant != null) {
      personalDataDto =
          PersonalDataDto.builder()
              .firstName(applicant.getFirstName())
              .lastName(applicant.getLastName())
              .documentNumber(applicant.getDocumentNumber())
              .birthDate(applicant.getBirthDate())
              .phoneNumber(applicant.getPhoneNumber())
              .email(applicant.getEmail())
              .build();
    }

    AcademicBackgroundDto academicBgDto = null;
    ApplicantEducationBackground bg = entity.getEducationBackground();
    if (bg != null) {
      academicBgDto =
          AcademicBackgroundDto.builder()
              .secondarySchool(bg.getSecondarySchool())
              .schoolOrigin(bg.getSchoolOrigin())
              .currentGradeYear(bg.getCurrentGradeYear())
              .secondaryCompleted(bg.isSecondaryCompleted())
              .secondaryDegreeTitle(bg.getSecondaryDegreeTitle())
              .build();
    }

    HealthInclusionDto healthDto = null;
    ApplicantHealthInclusion health = entity.getHealthInclusion();
    if (health != null) {
      healthDto =
          HealthInclusionDto.builder()
              .receivesReasonableAdjustments(health.isReceivesReasonableAdjustments())
              .adjustmentDetails(health.getAdjustmentDetails())
              .build();
    }

    ResponsibleDto responsibleDto = null;
    ApplicantResponsible resp = entity.getResponsible();
    if (resp != null) {
      responsibleDto =
          ResponsibleDto.builder()
              .fullName(resp.getFullName())
              .documentNumber(resp.getDocumentNumber())
              .occupation(resp.getOccupation())
              .phoneNumber(resp.getPhoneNumber())
              .email(resp.getEmail())
              .educationLevel(resp.getEducationLevel())
              .build();
    }

    PreferenceDto preferenceDto = null;
    ApplicantPreference pref = entity.getPreference();
    if (pref != null) {
      preferenceDto =
          PreferenceDto.builder()
              .preferredShift(pref.getPreferredShift())
              .allowsImageUse(pref.isAllowsImageUse())
              .isReenrolling(pref.isReenrolling())
              .previousTeacher(pref.getPreviousTeacher())
              .build();
    }

    AcademicSpaceSelectionDto spaceSelectionDto = null;
    InstrumentSelectionDto instrumentSelectionDto = null;
    if (entity.getSelectedSpaces() != null && !entity.getSelectedSpaces().isEmpty()) {
      List<UUID> spaceIds =
          entity.getSelectedSpaces().stream().map(s -> s.getStudyPlanSpace().getId()).toList();
      spaceSelectionDto = new AcademicSpaceSelectionDto(spaceIds);

      Map<UUID, UUID> instMap =
          entity.getSelectedSpaces().stream()
              .filter(s -> s.getInstrument() != null)
              .collect(
                  Collectors.toMap(
                      s -> s.getStudyPlanSpace().getId(), s -> s.getInstrument().getId()));
      instrumentSelectionDto = new InstrumentSelectionDto(instMap);
    }

    CareerSelectionDto careerDto = null;
    if (entity.getStudyPlan() != null && entity.getStudyPlan().getTrainingPath() != null) {
      careerDto = new CareerSelectionDto(entity.getStudyPlan().getTrainingPath().getId());
    }

    List<AttachmentDto> attachmentsList = new ArrayList<>();
    if (entity.getAttachments() != null) {
      attachmentsList =
          entity.getAttachments().stream()
              .filter(att -> att.getDeletedAt() == null)
              .map(
                  att ->
                      AttachmentDto.builder()
                          .id(att.getId())
                          .attachmentType(
                              att.getAttachmentType() != null
                                  ? att.getAttachmentType().name()
                                  : null)
                          .originalFileName(att.getOriginalFileName())
                          .storagePath(att.getStoragePath())
                          .contentType(att.getContentType())
                          .fileSize(att.getFileSize())
                          .createdAt(att.getCreatedAt())
                          .build())
              .toList();
    }

    return EnrollmentDraftData.builder()
        .personalData(personalDataDto != null ? personalDataDto : new PersonalDataDto())
        .academicBackground(academicBgDto != null ? academicBgDto : new AcademicBackgroundDto())
        .healthInclusion(healthDto != null ? healthDto : new HealthInclusionDto())
        .responsible(responsibleDto != null ? responsibleDto : new ResponsibleDto())
        .preference(preferenceDto != null ? preferenceDto : new PreferenceDto())
        .careerSelection(careerDto != null ? careerDto : new CareerSelectionDto())
        .academicSpaceSelection(
            spaceSelectionDto != null ? spaceSelectionDto : new AcademicSpaceSelectionDto())
        .instrumentSelection(
            instrumentSelectionDto != null ? instrumentSelectionDto : new InstrumentSelectionDto())
        .attachments(attachmentsList)
        .build();
  }
}
