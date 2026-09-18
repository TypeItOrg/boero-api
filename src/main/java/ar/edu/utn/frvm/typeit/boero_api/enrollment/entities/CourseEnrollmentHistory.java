package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_enrollment_histories")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CourseEnrollmentHistory extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "course_enrollment_history_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_enrollment_id", nullable = false)
  private CourseEnrollment courseEnrollment;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 40)
  private CourseEnrollmentStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", length = 40)
  private CourseEnrollmentStatus newStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_academic_status", length = 30)
  private AcademicEnrollmentStatus previousAcademicStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_academic_status", length = 30)
  private AcademicEnrollmentStatus newAcademicStatus;

  @Column(nullable = false, length = 80)
  private String operation;

  @Column private String reason;

  @Column(name = "authority_person_id")
  private UUID authorityPersonId;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  public static CourseEnrollmentHistory create(
      final Institution institution,
      final CourseEnrollment courseEnrollment,
      final CourseEnrollmentStatus previousStatus,
      final CourseEnrollmentStatus newStatus,
      final AcademicEnrollmentStatus previousAcademicStatus,
      final AcademicEnrollmentStatus newAcademicStatus,
      final String operation,
      final String reason,
      final UUID authorityPersonId,
      final Instant changedAt) {
    return CourseEnrollmentHistory.builder()
        .institution(institution)
        .courseEnrollment(courseEnrollment)
        .previousStatus(previousStatus)
        .newStatus(newStatus)
        .previousAcademicStatus(previousAcademicStatus)
        .newAcademicStatus(newAcademicStatus)
        .operation(operation)
        .reason(reason)
        .authorityPersonId(authorityPersonId)
        .changedAt(changedAt)
        .build();
  }
}
