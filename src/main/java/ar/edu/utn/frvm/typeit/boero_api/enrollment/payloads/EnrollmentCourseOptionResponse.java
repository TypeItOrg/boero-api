package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "courseId",
      "studyPlanSpaceId",
      "academicSpaceName",
      "academicLevelName",
      "studyPlanName",
      "trainingPathName",
      "format",
      "instrumentId",
      "instrumentName",
      "requirementType",
      "approvalMode",
      "instrumental",
      "hasCapacity",
      "eligibility",
      "academicYear"
    })
public record EnrollmentCourseOptionResponse(
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    @Schema(nullable = true) String academicLevelName,
    String studyPlanName,
    String trainingPathName,
    String format,
    @Schema(nullable = true) UUID instrumentId,
    @Schema(nullable = true) String instrumentName,
    String requirementType,
    String approvalMode,
    boolean instrumental,
    boolean hasCapacity,
    AcademicEligibilityResponse eligibility,
    int academicYear) {

  public static EnrollmentCourseOptionResponse from(
      final Course course,
      final boolean hasCapacity,
      final AcademicEligibilityResponse eligibility) {
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
        hasCapacity,
        eligibility,
        course.getAcademicYear().getYear());
  }
}
