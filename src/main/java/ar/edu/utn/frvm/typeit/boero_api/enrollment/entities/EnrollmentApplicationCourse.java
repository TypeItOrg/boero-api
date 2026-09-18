package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.WaitlistReason;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enrollment_application_courses")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EnrollmentApplicationCourse extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "enrollment_application_course_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_application_id", nullable = false)
  private EnrollmentApplication enrollmentApplication;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preferred_teacher_id")
  private Person preferredTeacher;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private EnrollmentApplicationCourseStatus status = EnrollmentApplicationCourseStatus.PENDING;

  @Column(name = "requested_at")
  private Instant requestedAt;

  @Column(name = "submitted_with_capacity")
  private Boolean submittedWithCapacity;

  @Column(name = "waitlist_number")
  private Integer waitlistNumber;

  @Column(name = "waitlisted_at")
  private Instant waitlistedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "waitlist_reason", length = 80)
  private WaitlistReason waitlistReason;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_by_person_id")
  private UUID resolvedByPersonId;

  @Column(name = "resolution_reason_code", length = 80)
  private String resolutionReasonCode;

  @Column(name = "resolution_reason_text")
  private String resolutionReasonText;

  @Version
  @Column(nullable = false)
  private long version;

  public static EnrollmentApplicationCourse create(
      final Institution institution,
      final EnrollmentApplication application,
      final Course course,
      final Person preferredTeacher) {
    return EnrollmentApplicationCourse.builder()
        .institution(institution)
        .enrollmentApplication(application)
        .course(course)
        .preferredTeacher(preferredTeacher)
        .status(EnrollmentApplicationCourseStatus.PENDING)
        .build();
  }

  public void markRequested(final Instant requestedAt, final boolean submittedWithCapacity) {
    this.requestedAt = requestedAt;
    this.submittedWithCapacity = submittedWithCapacity;
  }

  public void changePreferredTeacher(final Person preferredTeacher) {
    this.preferredTeacher = preferredTeacher;
  }

  public void waitlist(
      final int waitlistNumber, final WaitlistReason reason, final Instant waitlistedAt) {
    status = EnrollmentApplicationCourseStatus.WAITLISTED;
    this.waitlistNumber = waitlistNumber;
    this.waitlistReason = reason;
    this.waitlistedAt = waitlistedAt;
  }

  public void enroll(final Instant resolvedAt, final UUID resolvedByPersonId) {
    status = EnrollmentApplicationCourseStatus.ENROLLED;
    this.resolvedAt = resolvedAt;
    this.resolvedByPersonId = resolvedByPersonId;
  }

  public void reject(
      final String reasonCode,
      final String reasonText,
      final Instant resolvedAt,
      final UUID resolvedByPersonId) {
    status = EnrollmentApplicationCourseStatus.REJECTED;
    this.resolutionReasonCode = reasonCode;
    this.resolutionReasonText = reasonText;
    this.resolvedAt = resolvedAt;
    this.resolvedByPersonId = resolvedByPersonId;
  }

  public void cancel(final Instant resolvedAt, final UUID resolvedByPersonId) {
    status = EnrollmentApplicationCourseStatus.CANCELLED;
    this.resolvedAt = resolvedAt;
    this.resolvedByPersonId = resolvedByPersonId;
  }
}
