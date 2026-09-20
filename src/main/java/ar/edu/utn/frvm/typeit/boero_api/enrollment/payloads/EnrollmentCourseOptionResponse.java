package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import java.util.UUID;

public record EnrollmentCourseOptionResponse(
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    String academicLevelName,
    String studyPlanName,
    String trainingPathName,
    String format,
    UUID instrumentId,
    String instrumentName,
    String requirementType,
    String approvalMode,
    boolean instrumental,
    boolean hasCapacity) {

  public static EnrollmentCourseOptionResponse from(
      final Course course, final boolean hasCapacity) {
    final var metadata = CourseEnrollmentCourseMetadata.from(course);
    return new EnrollmentCourseOptionResponse(
        metadata.courseId(),
        metadata.studyPlanSpaceId(),
        metadata.academicSpaceName(),
        metadata.academicLevelName(),
        metadata.studyPlanName(),
        metadata.trainingPathName(),
        metadata.format(),
        metadata.instrumentId(),
        metadata.instrumentName(),
        metadata.requirementType(),
        metadata.approvalMode(),
        metadata.instrumental(),
        hasCapacity);
  }
}
