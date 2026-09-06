package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Builder(access = AccessLevel.PACKAGE)
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "study_plan_id", nullable = false)
  private StudyPlan studyPlan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_period_id", nullable = false)
  private EnrollmentPeriod enrollmentPeriod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EnrollmentApplicationStatus status;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @OneToOne(
      mappedBy = "enrollmentApplication",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private ApplicantEducationBackground educationBackground;

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

  public void submit() {
    if (isEditable()) {
      status = EnrollmentApplicationStatus.SUBMITTED;
      return;
    }
    throw new InvalidEnrollmentApplicationStateException(
        EnrollmentMessages.APPLICATION_CANNOT_SUBMIT);
  }

  public void approve(final LocalDateTime resolvedAt) {
    ensurePendingEvaluation();
    status = EnrollmentApplicationStatus.APPROVED;
    this.resolvedAt = resolvedAt;
  }

  public void reject(final String rejectionReason, final LocalDateTime resolvedAt) {
    ensurePendingEvaluation();
    if (rejectionReason == null || rejectionReason.isBlank()) {
      throw new MissingRejectionReasonException();
    }
    status = EnrollmentApplicationStatus.REJECTED;
    this.rejectionReason = rejectionReason;
    this.resolvedAt = resolvedAt;
  }

  public boolean isEditable() {
    return status == EnrollmentApplicationStatus.DRAFT;
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
    educationBackground.updateSecondarySchool(secondarySchool);
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
