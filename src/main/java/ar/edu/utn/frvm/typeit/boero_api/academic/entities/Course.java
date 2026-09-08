package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.InvalidAcademicStateException;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "courses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "courses_institution_id_id_unique",
            columnNames = {"institution_id", "course_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class Course extends SoftDeletable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "course_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "study_plan_id", nullable = false)
  private StudyPlan studyPlan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "academic_space_id", nullable = false)
  private AcademicSpace academicSpace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CourseStatus status;

  public static Course create(
      final Institution institution,
      final StudyPlan studyPlan,
      final AcademicSpace academicSpace,
      final AcademicYear academicYear) {
    return Course.builder()
        .institution(institution)
        .studyPlan(studyPlan)
        .academicSpace(academicSpace)
        .academicYear(academicYear)
        .status(CourseStatus.ACTIVE)
        .build();
  }

  public void activate() {
    if (status == CourseStatus.CLOSED) {
      throw new InvalidAcademicStateException();
    }
    ensureParentsActive();
    status = CourseStatus.ACTIVE;
  }

  public void deactivate() {
    if (status == CourseStatus.CLOSED) {
      throw new InvalidAcademicStateException();
    }
    status = CourseStatus.INACTIVE;
  }

  public void close() {
    if (status == CourseStatus.CLOSED) {
      return;
    }
    status = CourseStatus.CLOSED;
  }

  public boolean isActive() {
    return status == CourseStatus.ACTIVE;
  }

  public boolean isClosed() {
    return status == CourseStatus.CLOSED;
  }

  public void updateStatus(final CourseStatus target) {
    if (target == status) {
      return;
    }
    if (target == CourseStatus.CLOSED) {
      close();
      return;
    }
    if (status == CourseStatus.CLOSED) {
      throw new InvalidAcademicStateException();
    }
    final boolean validTransition =
        (status == CourseStatus.ACTIVE && target == CourseStatus.INACTIVE)
            || (status == CourseStatus.INACTIVE && target == CourseStatus.ACTIVE);
    if (!validTransition) {
      throw new InvalidAcademicStateException();
    }
    if (target == CourseStatus.ACTIVE) {
      ensureParentsActive();
    }
    status = target;
  }

  public boolean delete(final LocalDateTime deletedAt) {
    if (status == CourseStatus.CLOSED) {
      throw new InvalidAcademicStateException();
    }
    if (status == CourseStatus.ACTIVE) {
      throw new InvalidAcademicStateException();
    }
    return markDeleted(deletedAt);
  }

  @Override
  public boolean restore() {
    if (status == CourseStatus.CLOSED) {
      throw new InvalidAcademicStateException();
    }
    if (isDeleted()) {
      ensureParentsActive();
    }
    return super.restore();
  }

  private void ensureAcademicYearActive() {
    if (academicYear.getStatus() != AcademicYearStatus.ACTIVE) {
      throw new InvalidAcademicStateException();
    }
  }

  private void ensureParentsActive() {
    if (studyPlan.getStatus() != StudyPlanStatus.ACTIVE) {
      throw new InvalidAcademicStateException();
    }
    ensureAcademicYearActive();
  }
}
