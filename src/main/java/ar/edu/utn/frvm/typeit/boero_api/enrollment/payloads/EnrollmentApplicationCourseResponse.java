package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentApplicationCourseResponse(
    UUID applicationCourseId,
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    String academicLevelName,
    String studyPlanName,
    int studyPlanVersion,
    String trainingPathName,
    UUID instrumentId,
    String instrumentName,
    UUID preferredTeacherId,
    EnrollmentApplicationCourseStatus status,
    Instant requestedAt,
    Boolean submittedWithCapacity,
    Integer waitlistNumber,
    Instant waitlistedAt,
    String waitlistReason,
    Instant resolvedAt,
    UUID resolvedByPersonId,
    String resolutionReasonCode,
    String resolutionReasonText,
    long version,
    boolean withinPeriodScope,
    boolean periodOpen,
    UUID enrollmentPeriodId,
    Instant enrollmentDeadline,
    int academicYear) {

  public static EnrollmentApplicationCourseResponse from(
      final EnrollmentApplicationCourse selection) {
    final var course = selection.getCourse();
    final var metadata = CourseEnrollmentCourseMetadata.from(course);
    final var preferredTeacher = selection.getPreferredTeacher();
    return new EnrollmentApplicationCourseResponse(
        selection.getId(),
        metadata.courseId(),
        metadata.studyPlanSpaceId(),
        metadata.academicSpaceName(),
        metadata.academicLevelName(),
        metadata.studyPlanName(),
        metadata.studyPlanVersion(),
        metadata.trainingPathName(),
        metadata.instrumentId(),
        metadata.instrumentName(),
        preferredTeacher == null ? null : preferredTeacher.getId(),
        selection.getStatus(),
        selection.getRequestedAt(),
        selection.getSubmittedWithCapacity(),
        selection.getWaitlistNumber(),
        selection.getWaitlistedAt(),
        selection.getWaitlistReason() == null ? null : selection.getWaitlistReason().name(),
        selection.getResolvedAt(),
        selection.getResolvedByPersonId(),
        selection.getResolutionReasonCode(),
        selection.getResolutionReasonText(),
        selection.getVersion(),
        selection.getCourse().getStudyPlanSpace() != null
            && selection.getEnrollmentPeriod().includes(selection.getCourse().getStudyPlanSpace()),
        selection.getEnrollmentPeriod().isOpenAt(Instant.now()),
        selection.getEnrollmentPeriod().getId(),
        selection.getEnrollmentPeriod().getEndDate(),
        course.getAcademicYear().getYear());
  }
}
