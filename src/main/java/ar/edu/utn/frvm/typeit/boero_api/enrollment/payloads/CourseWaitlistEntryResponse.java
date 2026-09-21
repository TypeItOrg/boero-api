package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "applicationId",
      "applicationCourse",
      "hasCapacity",
      "applicationCourseId",
      "courseId",
      "trainingPathId",
      "waitlistNumber",
      "applicantName",
      "applicantDocumentNumber",
      "requestedAt",
      "waitlistedAt",
      "originalReason",
      "currentSituation",
      "preferredShift",
      "preferredTeacherId"
    })
public record CourseWaitlistEntryResponse(
    UUID applicationId,
    EnrollmentApplicationCourseResponse applicationCourse,
    boolean hasCapacity,
    UUID applicationCourseId,
    UUID courseId,
    UUID trainingPathId,
    Integer waitlistNumber,
    String applicantName,
    String applicantDocumentNumber,
    @Schema(nullable = true) Instant requestedAt,
    Instant waitlistedAt,
    @Schema(nullable = true) String originalReason,
    String currentSituation,
    @Schema(nullable = true) String preferredShift,
    @Schema(nullable = true) UUID preferredTeacherId) {

  public static CourseWaitlistEntryResponse from(
      final EnrollmentApplicationCourse selection, final boolean hasCapacity) {
    final var application = selection.getEnrollmentApplication();
    final var person = application.getApplicantPerson();
    final var preference = application.getPreference();
    return new CourseWaitlistEntryResponse(
        application.getId(),
        EnrollmentApplicationCourseResponse.from(selection),
        hasCapacity,
        selection.getId(),
        selection.getCourse().getId(),
        selection.getCourse().getStudyPlan().getTrainingPath().getId(),
        selection.getWaitlistNumber(),
        person.getFirstName() + " " + person.getLastName(),
        person.getDocumentNumber(),
        selection.getRequestedAt(),
        selection.getWaitlistedAt(),
        selection.getWaitlistReason() == null ? null : selection.getWaitlistReason().name(),
        selection.getStatus().name(),
        preference == null ? null : preference.getPreferredShift(),
        selection.getPreferredTeacher() == null ? null : selection.getPreferredTeacher().getId());
  }
}
