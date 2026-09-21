package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.MissingRejectionReasonException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "enrollment_applications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "enrollment_applications_institution_id_id_unique",
            columnNames = {"institution_id", "enrollment_application_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EnrollmentApplication extends SoftDeletable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "enrollment_application_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "applicant_person_id", nullable = false)
  private Person applicantPerson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "study_plan_id")
  private StudyPlan studyPlan;

  @Column(name = "study_plan_id", insertable = false, updatable = false)
  private UUID legacyStudyPlanId;

  @Transient private boolean legacyPlanLoaded;

  @PostLoad
  private void detectLegacyPlan() {
    legacyPlanLoaded = studyPlan != null;
  }

  public boolean hasLegacyStudyPlan() {
    return legacyPlanLoaded || legacyStudyPlanId != null || studyPlan != null;
  }

  @Setter(AccessLevel.NONE)
  @Column(name = "training_path_id", nullable = false)
  private UUID trainingPathId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_path_id", insertable = false, updatable = false)
  private TrainingPath trainingPath;

  @PrePersist
  @PreUpdate
  private void synchronizeTrainingPath() {
    if (trainingPathId == null && studyPlan != null) {
      trainingPathId = studyPlan.getTrainingPath().getId();
    }
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id")
  private AcademicYear academicYear;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_period_id")
  private EnrollmentPeriod enrollmentPeriod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EnrollmentApplicationStatus status;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_by_person_id")
  private UUID resolvedByPersonId;

  @OneToOne(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private ApplicantEducationBackground educationBackground;

  @OneToOne(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private ApplicantHealthInclusion healthInclusion;

  @OneToOne(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private ApplicantResponsible responsible;

  @OneToOne(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private ApplicantPreference preference;

  @OneToMany(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Builder.Default
  private List<EnrollmentAttachment> attachments = new ArrayList<>();

  @OneToMany(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Builder.Default
  private List<EnrollmentApplicationSpace> selectedSpaces = new ArrayList<>();

  @OneToMany(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Builder.Default
  private List<EnrollmentApplicationCourse> courseSelections = new ArrayList<>();

  public void setEducationBackground(ApplicantEducationBackground educationBackground) {
    this.educationBackground = educationBackground;

    if (educationBackground != null) {
      educationBackground.setEnrollmentApplication(this);
    }
  }

  public void setHealthInclusion(ApplicantHealthInclusion healthInclusion) {
    this.healthInclusion = healthInclusion;

    if (healthInclusion != null) {
      healthInclusion.setEnrollmentApplication(this);
    }
  }

  public void setResponsible(ApplicantResponsible responsible) {
    this.responsible = responsible;

    if (responsible != null) {
      responsible.setEnrollmentApplication(this);
    }
  }

  public void setPreference(ApplicantPreference preference) {
    this.preference = preference;

    if (preference != null) {
      preference.setEnrollmentApplication(this);
    }
  }

  public void addAttachment(EnrollmentAttachment attachment) {
    attachments.add(attachment);
    attachment.setEnrollmentApplication(this);
  }

  public void addSelectedSpace(EnrollmentApplicationSpace selectedSpace) {
    selectedSpaces.add(selectedSpace);
    selectedSpace.setEnrollmentApplication(this);
  }

  public void clearSelectedSpaces() {
    selectedSpaces.clear();
  }

  public void addCourseSelection(final EnrollmentApplicationCourse selection) {
    courseSelections.add(selection);
  }

  public void clearCourseSelections() {
    courseSelections.clear();
  }

  public void changeStudyPlan(final StudyPlan studyPlan) {
    if (!isEditable()) {
      throw new ApplicationNotEditableException(id);
    }

    this.studyPlan = studyPlan;
    this.trainingPathId = studyPlan.getTrainingPath().getId();
    this.trainingPath = studyPlan.getTrainingPath();
    clearSelectedSpaces();
  }

  public void changeTrainingPath(final TrainingPath trainingPath) {
    if (!isEditable()) {
      throw new ApplicationNotEditableException(id);
    }

    this.trainingPath = trainingPath;
    this.trainingPathId = trainingPath.getId();
    this.studyPlan = null;
    clearSelectedSpaces();
    clearCourseSelections();
  }

  public static EnrollmentApplication create(
      final Institution institution,
      final Person applicantPerson,
      final StudyPlan studyPlan,
      final AcademicYear academicYear,
      final EnrollmentPeriod enrollmentPeriod) {
    return EnrollmentApplication.builder()
        .institution(institution)
        .applicantPerson(applicantPerson)
        .studyPlan(studyPlan)
        .academicYear(academicYear)
        .enrollmentPeriod(enrollmentPeriod)
        .status(EnrollmentApplicationStatus.DRAFT)
        .build();
  }

  public AcademicYear commonAcademicYear() {
    if (academicYear != null) {
      return academicYear;
    }
    if (courseSelections.isEmpty()) {
      return null;
    }
    final var year = courseSelections.getFirst().getCourse().getAcademicYear();
    return courseSelections.stream()
            .allMatch(
                selection -> selection.getCourse().getAcademicYear().getId().equals(year.getId()))
        ? year
        : null;
  }

  public void useCoursePeriods() {
    if (status != EnrollmentApplicationStatus.DRAFT) {
      throw new IllegalStateException("Only drafts can change their enrollment scope");
    }
    enrollmentPeriod = null;
    academicYear = null;
  }

  public static EnrollmentApplication createForTrainingPath(
      final Institution institution,
      final Person applicantPerson,
      final TrainingPath trainingPath) {
    return EnrollmentApplication.builder()
        .institution(institution)
        .applicantPerson(applicantPerson)
        .trainingPathId(trainingPath.getId())
        .trainingPath(trainingPath)
        .status(EnrollmentApplicationStatus.DRAFT)
        .build();
  }

  public static EnrollmentApplication createForTrainingPath(
      final Institution institution,
      final Person applicantPerson,
      final TrainingPath trainingPath,
      final AcademicYear academicYear,
      final EnrollmentPeriod enrollmentPeriod) {
    return EnrollmentApplication.builder()
        .institution(institution)
        .applicantPerson(applicantPerson)
        .trainingPathId(trainingPath.getId())
        .trainingPath(trainingPath)
        .academicYear(academicYear)
        .enrollmentPeriod(enrollmentPeriod)
        .status(EnrollmentApplicationStatus.DRAFT)
        .build();
  }

  public void submit() {
    if (isEditable()) {
      status = EnrollmentApplicationStatus.SUBMITTED;

      return;
    }

    throw new InvalidEnrollmentApplicationStateException(
        EnrollmentMessages.APPLICATION_CANNOT_SUBMIT);
  }

  public void cancel() {
    if (!isEditable()) {
      throw new ApplicationNotEditableException(id);
    }

    status = EnrollmentApplicationStatus.CANCELLED;
  }

  public void updateEducationBackground(final String secondarySchool) {
    if (!isEditable()) {
      throw new InvalidEnrollmentApplicationStateException(
          EnrollmentMessages.APPLICATION_NOT_EDITABLE);
    }

    if (educationBackground == null) {
      educationBackground =
          ApplicantEducationBackground.builder()
              .enrollmentApplication(this)
              .secondarySchool(secondarySchool)
              .build();

      return;
    }

    educationBackground.setSecondarySchool(secondarySchool);
  }

  public boolean isEditable() {
    return status == EnrollmentApplicationStatus.DRAFT && getDeletedAt() == null;
  }

  public boolean isPendingEvaluation() {
    return status == EnrollmentApplicationStatus.SUBMITTED;
  }

  public boolean isApproved() {
    return status == EnrollmentApplicationStatus.APPROVED;
  }

  public boolean isResolved() {
    return status == EnrollmentApplicationStatus.APPROVED
        || status == EnrollmentApplicationStatus.REJECTED
        || status == EnrollmentApplicationStatus.CANCELLED;
  }

  public TrainingPath getTrainingPath() {
    if (trainingPath != null) {
      return trainingPath;
    }

    return studyPlan == null ? null : studyPlan.getTrainingPath();
  }

  public void approve(final Instant resolvedAt, final UUID resolvedByPersonId) {
    ensurePendingEvaluation();
    status = EnrollmentApplicationStatus.APPROVED;
    this.resolvedAt = resolvedAt;
    this.resolvedByPersonId = resolvedByPersonId;
  }

  public void reject(
      final String rejectionReason, final Instant resolvedAt, final UUID resolvedByPersonId) {
    ensurePendingEvaluation();

    if (rejectionReason == null || rejectionReason.isBlank()) {
      throw new MissingRejectionReasonException();
    }

    status = EnrollmentApplicationStatus.REJECTED;
    this.rejectionReason = rejectionReason;
    this.resolvedAt = resolvedAt;
    this.resolvedByPersonId = resolvedByPersonId;
  }

  private void ensurePendingEvaluation() {
    if (isResolved()) {
      throw new InvalidEnrollmentApplicationStateException(
          EnrollmentMessages.APPLICATION_ALREADY_RESOLVED);
    }

    if (!isPendingEvaluation()) {
      throw new InvalidEnrollmentApplicationStateException(
          EnrollmentMessages.APPLICATION_NOT_PENDING_EVALUATION);
    }
  }
}
