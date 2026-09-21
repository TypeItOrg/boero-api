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
      "periodOpen"
    })
public record EnrollmentApplicationResponse(
    @Schema(nullable = true) UUID applicationId,
    @Schema(nullable = true) UUID institutionId,
    @Schema(nullable = true) UUID personId,
    @Schema(nullable = true) String applicantFirstName,
    @Schema(nullable = true) String applicantLastName,
    @Schema(nullable = true) String applicantDocumentNumber,
    @Schema(nullable = true) UUID trainingPathId,
    @Schema(nullable = true) UUID studyPlanId,
    @Schema(nullable = true) String studyPlanName,
    @Schema(nullable = true) String trainingPathName,
    @Schema(nullable = true) UUID academicYearId,
    @Schema(nullable = true) Integer academicYear,
    @Schema(nullable = true) UUID enrollmentPeriodId,
    @Schema(nullable = true) EnrollmentApplicationStatus status,
    @JsonProperty("isEditable") boolean isEditable,
    @Schema(nullable = true) EnrollmentDraftData data,
    @Schema(nullable = true) String secondarySchool,
    @Schema(nullable = true) String rejectionReason,
    @Schema(nullable = true) Instant resolvedAt,
    @Schema(nullable = true) UUID resolvedByPersonId,
    @Schema(nullable = true) Instant createdAt,
    @Schema(nullable = true) Instant updatedAt,
    @Schema(nullable = true) List<EnrollmentApplicationSpaceResponse> spaces,
    @Schema(nullable = true) List<EnrollmentApplicationCourseResponse> courses,
    @Schema(nullable = true) EnrollmentPeriodResponse enrollmentPeriod,
    boolean periodOpen) {
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
        false);
  }

  public EnrollmentApplicationResponse {
    spaces = spaces == null ? new ArrayList<>() : spaces;
    courses = courses == null ? new ArrayList<>() : courses;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getInstitutionId() {
    return institutionId;
  }

  public UUID getPersonId() {
    return personId;
  }

  public String getApplicantFirstName() {
    return applicantFirstName;
  }

  public String getApplicantLastName() {
    return applicantLastName;
  }

  public String getApplicantDocumentNumber() {
    return applicantDocumentNumber;
  }

  public UUID getTrainingPathId() {
    return trainingPathId;
  }

  public UUID getStudyPlanId() {
    return studyPlanId;
  }

  public String getStudyPlanName() {
    return studyPlanName;
  }

  public String getTrainingPathName() {
    return trainingPathName;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }

  public Integer getAcademicYear() {
    return academicYear;
  }

  public UUID getEnrollmentPeriodId() {
    return enrollmentPeriodId;
  }

  public EnrollmentApplicationStatus getStatus() {
    return status;
  }

  public EnrollmentDraftData getData() {
    return data;
  }

  public String getSecondarySchool() {
    return secondarySchool;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public UUID getResolvedByPersonId() {
    return resolvedByPersonId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<EnrollmentApplicationSpaceResponse> getSpaces() {
    return spaces;
  }

  public List<EnrollmentApplicationCourseResponse> getCourses() {
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
        .trainingPathId(
            application.getTrainingPath() != null ? application.getTrainingPath().getId() : null)
        .studyPlanId(application.getStudyPlan() != null ? application.getStudyPlan().getId() : null)
        .studyPlanName(
            application.getStudyPlan() != null ? application.getStudyPlan().getName() : null)
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
        .data(buildDraftData(application))
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

    if (entity.getTrainingPath() != null) {
      careerDto = new CareerSelectionDto(entity.getTrainingPath().getId());
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
