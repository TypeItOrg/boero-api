package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
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
import jakarta.persistence.Table;
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
@Table(name = "enrollment_applications")
@Getter
@Setter
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

  public boolean isEditable() {
    return status == EnrollmentApplicationStatus.DRAFT && getDeletedAt() == null;
  }
}
