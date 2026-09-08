package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "institutionName",
      "studyPlanId",
      "studyPlanName",
      "trainingPathId",
      "trainingPathName",
      "academicSpaceId",
      "academicSpaceName",
      "academicSpaceType",
      "academicSpaceFormat",
      "academicYearId",
      "year",
      "status",
      "active",
      "classes",
      "deletedAt"
    })
public record CourseResponse(
    UUID id,
    UUID institutionId,
    String institutionName,
    UUID studyPlanId,
    String studyPlanName,
    UUID trainingPathId,
    String trainingPathName,
    UUID academicSpaceId,
    String academicSpaceName,
    String academicSpaceType,
    String academicSpaceFormat,
    UUID academicYearId,
    int year,
    String status,
    boolean active,
    List<CourseClassResponse> classes,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static CourseResponse from(final Course course) {
    return from(course, List.of());
  }

  public static CourseResponse from(final Course course, final List<CourseClassResponse> classes) {
    final var space = course.getAcademicSpace();
    final var plan = course.getStudyPlan();
    final var path = plan.getTrainingPath();
    final var year = course.getAcademicYear();
    return new CourseResponse(
        course.getId(),
        course.getInstitution().getId(),
        course.getInstitution().getName(),
        plan.getId(),
        plan.getName(),
        path.getId(),
        path.getName(),
        space.getId(),
        space.getName(),
        space.getType().name(),
        space.getFormat().name(),
        year.getId(),
        year.getYear(),
        course.getStatus().name(),
        course.isActive(),
        classes,
        course.getDeletedAt());
  }
}
