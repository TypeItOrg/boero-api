package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentSource;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseEnrollmentResponse(
    UUID id,
    UUID institutionId,
    UUID studentId,
    String studentName,
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    String academicLevelName,
    String studyPlanName,
    String trainingPathName,
    UUID instrumentId,
    String instrumentName,
    UUID courseClassId,
    CourseEnrollmentSource source,
    CourseEnrollmentStatus status,
    AcademicEnrollmentStatus academicStatus,
    Instant enrolledAt,
    Instant completedAt,
    Instant withdrawnAt,
    Long version,
    List<CourseEnrollmentScheduleResponse> schedules) {

  public static CourseEnrollmentResponse from(
      final CourseEnrollment enrollment, final List<CourseEnrollmentScheduleResponse> schedules) {
    final var course = enrollment.getCourse();
    final var metadata = CourseEnrollmentCourseMetadata.from(course);
    final var student = enrollment.getStudent();
    final var person = student.getPerson();
    return new CourseEnrollmentResponse(
        enrollment.getId(),
        enrollment.getInstitution().getId(),
        student.getId(),
        person.getFirstName() + " " + person.getLastName(),
        metadata.courseId(),
        metadata.studyPlanSpaceId(),
        metadata.academicSpaceName(),
        metadata.academicLevelName(),
        metadata.studyPlanName(),
        metadata.trainingPathName(),
        metadata.instrumentId(),
        metadata.instrumentName(),
        enrollment.getCourseClass().getId(),
        enrollment.getSource(),
        enrollment.getStatus(),
        enrollment.getAcademicStatus(),
        enrollment.getEnrolledAt(),
        enrollment.getCompletedAt(),
        enrollment.getWithdrawnAt(),
        enrollment.getVersion(),
        schedules);
  }
}
