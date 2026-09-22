package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EducationLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "applicant_education_backgrounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ApplicantEducationBackground extends SoftDeletable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "applicant_education_background_id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_application_id", nullable = false)
  private EnrollmentApplication enrollmentApplication;

  @Column(name = "secondary_school", length = 255)
  private String secondarySchool;

  @Column(name = "school_origin", length = 150)
  private String schoolOrigin;

  @Column(name = "current_grade_year", length = 50)
  private String currentGradeYear;

  @Column(name = "currently_studying")
  private Boolean currentlyStudying;

  @Enumerated(EnumType.STRING)
  @Column(name = "education_level", length = 30)
  private EducationLevel educationLevel;

  @Column(name = "level_completed")
  private Boolean levelCompleted;

  @Column(name = "secondary_completed")
  private Boolean secondaryCompleted;

  @Column(name = "secondary_degree_title", length = 150)
  private String secondaryDegreeTitle;

  public void updateSchooling(
      final Boolean currentlyStudying,
      final EducationLevel educationLevel,
      final String schoolOrigin,
      final String currentGradeYear,
      final Boolean levelCompleted,
      final Boolean secondaryCompleted,
      final String secondaryDegreeTitle) {
    this.currentlyStudying = currentlyStudying;
    this.educationLevel = educationLevel;
    this.schoolOrigin = normalize(schoolOrigin);
    this.currentGradeYear = normalize(currentGradeYear);
    this.levelCompleted = levelCompleted;
    this.secondaryCompleted = secondaryCompleted;
    this.secondaryDegreeTitle = normalize(secondaryDegreeTitle);

    normalizeSchoolingAnswers();
  }

  private void normalizeSchoolingAnswers() {
    if (currentlyStudying == null) {
      educationLevel = null;
      schoolOrigin = null;
      currentGradeYear = null;
      levelCompleted = null;
      secondaryCompleted = null;
      secondaryDegreeTitle = null;

      return;
    }

    if (Boolean.TRUE.equals(currentlyStudying)) {
      levelCompleted = null;
    } else {
      currentGradeYear = null;
    }

    if (educationLevel == null) {
      levelCompleted = null;
      secondaryCompleted = null;
      secondaryDegreeTitle = null;

      return;
    }

    if (educationLevel == EducationLevel.NO_SCHOOLING) {
      schoolOrigin = null;
      currentGradeYear = null;
      levelCompleted = null;
      secondaryCompleted = null;
      secondaryDegreeTitle = null;

      return;
    }

    if (!educationLevel.requiresSecondaryCompletionAnswer()) {
      secondaryCompleted = null;
      secondaryDegreeTitle = null;

      return;
    }

    if (educationLevel == EducationLevel.SECONDARY && Boolean.FALSE.equals(currentlyStudying)) {
      this.levelCompleted = secondaryCompleted;
    }

    if (!Boolean.TRUE.equals(secondaryCompleted)) {
      secondaryDegreeTitle = null;
    }
  }

  private static String normalize(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }
}
