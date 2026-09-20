package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentSource;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "studentId",
      "studentName",
      "courseId",
      "studyPlanSpaceId",
      "academicSpaceName",
      "academicLevelName",
      "studyPlanName",
      "trainingPathName",
      "instrumentId",
      "instrumentName",
      "courseClassId",
      "courseClassLabel",
      "source",
      "status",
      "academicStatus",
      "enrolledAt",
      "completedAt",
      "withdrawnAt",
      "version",
      "schedules"
    })
public record CourseEnrollmentResponse(
    UUID id,
    UUID institutionId,
    UUID studentId,
    String studentName,
    UUID courseId,
    UUID studyPlanSpaceId,
    String academicSpaceName,
    @Schema(nullable = true) String academicLevelName,
    String studyPlanName,
    String trainingPathName,
    @Schema(nullable = true) UUID instrumentId,
    @Schema(nullable = true) String instrumentName,
    UUID courseClassId,
    String courseClassLabel,
    CourseEnrollmentSource source,
    CourseEnrollmentStatus status,
    AcademicEnrollmentStatus academicStatus,
    Instant enrolledAt,
    @Schema(nullable = true) Instant completedAt,
    @Schema(nullable = true) Instant withdrawnAt,
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
        enrollment.getCourseClass().displayName(),
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
