package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantHealthInclusion;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantResponsible;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder(toBuilder = true)
@Schema(
    requiredProperties = {
      "applicationId",
      "institutionId",
      "personId",
      "applicantFirstName",
      "applicantLastName",
      "applicantDocumentNumber",
      "trainingPathId",
      "studyPlanId",
      "studyPlanName",
      "studyPlanVersion",
      "trainingPathName",
      "academicYearId",
      "academicYear",
      "enrollmentPeriodId",
      "status",
      "isEditable",
      "data",
      "secondarySchool",
      "rejectionReason",
      "resolvedAt",
      "resolvedByPersonId",
      "createdAt",
      "updatedAt",
      "spaces",
      "courses",
      "enrollmentPeriod",
      "periodOpen",
      "canReadAttachments",
      "documents",
      "canApproveProvisionally",
      "canConfirm",
      "documentRequests",
      "canRequestDocuments",
      "admissionHistory"
    })
public record EnrollmentApplicationResponse(
    @Schema(nullable = true) @Nullable UUID applicationId,
    @Schema(nullable = true) @Nullable UUID institutionId,
    @Schema(nullable = true) @Nullable UUID personId,
    @Schema(nullable = true) @Nullable String applicantFirstName,
    @Schema(nullable = true) @Nullable String applicantLastName,
    @Schema(nullable = true) @Nullable String applicantDocumentNumber,
    @Schema(nullable = true) @Nullable UUID trainingPathId,
    @Schema(nullable = true) @Nullable UUID studyPlanId,
    @Schema(nullable = true) @Nullable String studyPlanName,
    @Schema(nullable = true) @Nullable Integer studyPlanVersion,
    @Schema(nullable = true) @Nullable String trainingPathName,
    @Schema(nullable = true) @Nullable UUID academicYearId,
    @Schema(nullable = true) @Nullable Integer academicYear,
    @Schema(nullable = true) @Nullable UUID enrollmentPeriodId,
    @Schema(nullable = true) @Nullable EnrollmentApplicationStatus status,
    @JsonProperty("isEditable") boolean isEditable,
    @Schema(nullable = true) @Nullable EnrollmentDraftData data,
    @Schema(nullable = true) @Nullable String secondarySchool,
    @Schema(nullable = true) @Nullable String rejectionReason,
    @Schema(nullable = true) @Nullable Instant resolvedAt,
    @Schema(nullable = true) @Nullable UUID resolvedByPersonId,
    @Schema(nullable = true) @Nullable Instant createdAt,
    @Schema(nullable = true) @Nullable Instant updatedAt,
    @Schema(nullable = true) @Nullable List<EnrollmentApplicationSpaceResponse> spaces,
    @Schema(nullable = true) @Nullable List<EnrollmentApplicationCourseResponse> courses,
    @Schema(nullable = true) @Nullable EnrollmentPeriodResponse enrollmentPeriod,
    boolean periodOpen,
    boolean canReadAttachments,
    List<EnrollmentDocumentRequirementResponse> documents,
    boolean canApproveProvisionally,
    boolean canConfirm,
    List<EnrollmentAdmissionHistoryResponse> admissionHistory,
    boolean canRequestDocuments,
    List<EnrollmentDocumentRequestResponse> documentRequests) {
  public EnrollmentApplicationResponse() {
    this(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        new ArrayList<>(),
        new ArrayList<>(),
        null,
        false,
        false,
        List.of(),
        false,
        false,
        List.of(),
        false,
        List.of());
  }

  public EnrollmentApplicationResponse {
    documents = documents == null ? List.of() : documents;
    documentRequests = documentRequests == null ? List.of() : documentRequests;
    admissionHistory = admissionHistory == null ? List.of() : admissionHistory;
    spaces = spaces == null ? new ArrayList<>() : spaces;
    courses = courses == null ? new ArrayList<>() : courses;
  }

  public @Nullable UUID getApplicationId() {
    return applicationId;
  }

  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  public @Nullable UUID getPersonId() {
    return personId;
  }

  public @Nullable String getApplicantFirstName() {
    return applicantFirstName;
  }

  public @Nullable String getApplicantLastName() {
    return applicantLastName;
  }

  public @Nullable String getApplicantDocumentNumber() {
    return applicantDocumentNumber;
  }

  public @Nullable UUID getTrainingPathId() {
    return trainingPathId;
  }

  public @Nullable UUID getStudyPlanId() {
    return studyPlanId;
  }

  public @Nullable String getStudyPlanName() {
    return studyPlanName;
  }

  public @Nullable String getTrainingPathName() {
    return trainingPathName;
  }

  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  public @Nullable Integer getAcademicYear() {
    return academicYear;
  }

  public @Nullable UUID getEnrollmentPeriodId() {
    return enrollmentPeriodId;
  }

  public @Nullable EnrollmentApplicationStatus getStatus() {
    return status;
  }

  public @Nullable EnrollmentDraftData getData() {
    return data;
  }

  public @Nullable String getSecondarySchool() {
    return secondarySchool;
  }

  public @Nullable String getRejectionReason() {
    return rejectionReason;
  }

  public @Nullable Instant getResolvedAt() {
    return resolvedAt;
  }

  public @Nullable UUID getResolvedByPersonId() {
    return resolvedByPersonId;
  }

  public @Nullable Instant getCreatedAt() {
    return createdAt;
  }

  public @Nullable Instant getUpdatedAt() {
    return updatedAt;
  }

  public @Nullable List<EnrollmentApplicationSpaceResponse> getSpaces() {
    return spaces;
  }

  public @Nullable List<EnrollmentApplicationCourseResponse> getCourses() {
    return courses;
  }

  public static EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return from(application, true);
  }

  public static EnrollmentApplicationResponse summary(final EnrollmentApplication application) {
    final var trainingPath = application.getTrainingPath();
    return EnrollmentApplicationResponse.builder()
        .applicationId(application.getId())
        .institutionId(
            application.getInstitution() == null ? null : application.getInstitution().getId())
        .personId(
            application.getApplicantPerson() == null
                ? null
                : application.getApplicantPerson().getId())
        .applicantFirstName(
            application.getApplicantPerson() == null
                ? null
                : application.getApplicantPerson().getFirstName())
        .applicantLastName(
            application.getApplicantPerson() == null
                ? null
                : application.getApplicantPerson().getLastName())
        .applicantDocumentNumber(
            application.getApplicantPerson() == null
                ? null
                : application.getApplicantPerson().getDocumentNumber())
        .trainingPathId(trainingPath == null ? null : trainingPath.getId())
        .studyPlanId(application.getStudyPlan() == null ? null : application.getStudyPlan().getId())
        .studyPlanName(
            application.getStudyPlan() == null ? null : application.getStudyPlan().getName())
        .studyPlanVersion(
            application.getStudyPlan() == null
                ? null
                : application.getStudyPlan().getVersionNumber())
        .trainingPathName(trainingPath == null ? null : trainingPath.getName())
        .academicYearId(
            application.commonAcademicYear() == null
                ? null
                : application.commonAcademicYear().getId())
        .academicYear(
            application.commonAcademicYear() == null
                ? null
                : application.commonAcademicYear().getYear())
        .enrollmentPeriodId(
            application.getEnrollmentPeriod() == null
                ? null
                : application.getEnrollmentPeriod().getId())
        .enrollmentPeriod(
            application.getEnrollmentPeriod() == null
                ? null
                : EnrollmentPeriodResponse.from(application.getEnrollmentPeriod()))
        .periodOpen(
            application.getEnrollmentPeriod() != null
                && application.getEnrollmentPeriod().isOpenAt(Instant.now()))
        .status(application.getStatus())
        .isEditable(application.isEditable())
        .data(
            EnrollmentDraftData.builder()
                .careerSelection(
                    trainingPath == null ? null : new CareerSelectionDto(trainingPath.getId()))
                .build())
        .secondarySchool(
            application.getEducationBackground() == null
                ? null
                : application.getEducationBackground().getSecondarySchool())
        .rejectionReason(application.getRejectionReason())
        .resolvedAt(application.getResolvedAt())
        .resolvedByPersonId(application.getResolvedByPersonId())
        .createdAt(application.getCreatedAt())
        .updatedAt(application.getUpdatedAt())
        .spaces(new ArrayList<>())
        .courses(new ArrayList<>())
        .build();
  }

  public static EnrollmentApplicationResponse from(
      final EnrollmentApplication application, final boolean includeCourseDetails) {
    return from(application, includeCourseDetails, false);
  }

  public static EnrollmentApplicationResponse from(
      final EnrollmentApplication application,
      final boolean includeCourseDetails,
      final boolean includeAttachments) {
    return EnrollmentApplicationResponse.builder()
        .canReadAttachments(includeAttachments)
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
        .trainingPathId(
            application.getTrainingPath() != null ? application.getTrainingPath().getId() : null)
        .studyPlanId(application.getStudyPlan() != null ? application.getStudyPlan().getId() : null)
        .studyPlanName(
            application.getStudyPlan() != null ? application.getStudyPlan().getName() : null)
        .studyPlanVersion(
            application.getStudyPlan() == null
                ? null
                : application.getStudyPlan().getVersionNumber())
        .trainingPathName(
            application.getTrainingPath() != null ? application.getTrainingPath().getName() : null)
        .academicYearId(
            application.commonAcademicYear() != null
                ? application.commonAcademicYear().getId()
                : null)
        .academicYear(
            application.commonAcademicYear() != null
                ? application.commonAcademicYear().getYear()
                : null)
        .enrollmentPeriodId(
            application.getEnrollmentPeriod() != null
                ? application.getEnrollmentPeriod().getId()
                : null)
        .enrollmentPeriod(
            application.getEnrollmentPeriod() == null
                ? null
                : EnrollmentPeriodResponse.from(application.getEnrollmentPeriod()))
        .periodOpen(
            application.getEnrollmentPeriod() != null
                && application.getEnrollmentPeriod().isOpenAt(Instant.now()))
        .status(application.getStatus())
        .isEditable(application.isEditable())
        .data(buildDraftData(application, includeAttachments))
        .secondarySchool(
            application.getEducationBackground() != null
                ? application.getEducationBackground().getSecondarySchool()
                : null)
        .rejectionReason(application.getRejectionReason())
        .resolvedAt(application.getResolvedAt())
        .resolvedByPersonId(application.getResolvedByPersonId())
        .createdAt(application.getCreatedAt())
        .updatedAt(application.getUpdatedAt())
        .spaces(includeCourseDetails ? buildSpaces(application) : new ArrayList<>())
        .courses(
            !includeCourseDetails || application.getCourseSelections() == null
                ? new ArrayList<>()
                : application.getCourseSelections().stream()
                    .map(EnrollmentApplicationCourseResponse::from)
                    .toList())
        .build();
  }

  private static List<EnrollmentApplicationSpaceResponse> buildSpaces(
      final EnrollmentApplication entity) {
    if (entity.getSelectedSpaces() == null) {
      return new ArrayList<>();
    }

    return entity.getSelectedSpaces().stream()
        .map(
            s ->
                EnrollmentApplicationSpaceResponse.builder()
                    .studyPlanSpaceId(s.getStudyPlanSpace().getId())
                    .spaceName(s.getStudyPlanSpace().getAcademicSpace().getName())
                    .academicLevelName(
                        s.getStudyPlanSpace().getAcademicLevel() != null
                            ? s.getStudyPlanSpace().getAcademicLevel().getName()
                            : null)
                    .instrumentId(s.getInstrument() != null ? s.getInstrument().getId() : null)
                    .instrumentName(s.getInstrument() != null ? s.getInstrument().getName() : null)
                    .build())
        .toList();
  }

  private static EnrollmentDraftData buildDraftData(
      final EnrollmentApplication entity, final boolean includeAttachments) {
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
              .nationality(
                  applicant.getNationalityCountry() == null
                      ? null
                      : applicant.getNationalityCountry().getName())
              .build();
    }

    AcademicBackgroundDto academicBgDto = null;
    ApplicantEducationBackground bg = entity.getEducationBackground();

    if (bg != null) {
      academicBgDto =
          AcademicBackgroundDto.builder()
              .secondarySchool(bg.getSecondarySchool())
              .currentlyStudying(bg.getCurrentlyStudying())
              .educationLevel(bg.getEducationLevel())
              .schoolOrigin(bg.getSchoolOrigin())
              .currentGradeYear(bg.getCurrentGradeYear())
              .levelCompleted(bg.getLevelCompleted())
              .secondaryCompleted(bg.getSecondaryCompleted())
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

    if (entity.getTrainingPath() != null) {
      careerDto = new CareerSelectionDto(entity.getTrainingPath().getId());
    }

    List<AttachmentDto> attachmentsList = new ArrayList<>();

    if (includeAttachments && entity.getAttachments() != null) {
      attachmentsList =
          entity.getAttachments().stream()
              .filter(att -> att.getDeletedAt() == null && att.isCurrent())
              .map(
                  att ->
                      AttachmentDto.builder()
                          .id(att.getId())
                          .requirementId(att.getRequirement().getId())
                          .originalFileName(att.getOriginalFileName())
                          .storagePath(null)
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
        .courses(
            entity.getCourseSelections() == null
                ? new ArrayList<>()
                : entity.getCourseSelections().stream()
                    .map(
                        selection ->
                            new CourseSelectionDto(
                                selection.getCourse().getId(),
                                selection.getPreferredTeacher() == null
                                    ? null
                                    : selection.getPreferredTeacher().getId()))
                    .toList())
        .attachments(attachmentsList)
        .build();
  }
}
