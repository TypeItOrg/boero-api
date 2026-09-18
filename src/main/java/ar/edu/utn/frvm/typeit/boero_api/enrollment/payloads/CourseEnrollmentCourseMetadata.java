package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import java.util.UUID;

public record CourseEnrollmentCourseMetadata(
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    String academicLevelName,
    String studyPlanName,
    String trainingPathName,
    String format,
    UUID instrumentId,
    String instrumentName) {

  public static CourseEnrollmentCourseMetadata from(final Course course) {
    final var space = course.getStudyPlanSpace();
    final var academicSpace = space.getAcademicSpace();
    final var level = space.getAcademicLevel();
    final var plan = space.getStudyPlan();
    final var instrument = course.getInstrument();
    return new CourseEnrollmentCourseMetadata(
        course.getId(),
        space.getId(),
        academicSpace.getName(),
        level == null ? null : level.getName(),
        plan.getName(),
        plan.getTrainingPath().getName(),
        academicSpace.getFormat().name(),
        instrument == null ? null : instrument.getId(),
        instrument == null ? null : instrument.getName());
  }
}
