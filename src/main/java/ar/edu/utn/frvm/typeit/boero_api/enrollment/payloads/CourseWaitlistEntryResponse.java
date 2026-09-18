package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import java.time.Instant;
import java.util.UUID;

public record CourseWaitlistEntryResponse(
    UUID applicationCourseId,
    UUID courseId,
    Integer waitlistNumber,
    String applicantName,
    String applicantDocumentNumber,
    Instant requestedAt,
    Instant waitlistedAt,
    String originalReason,
    String currentSituation,
    String preferredShift,
    UUID preferredTeacherId) {

  public static CourseWaitlistEntryResponse from(final EnrollmentApplicationCourse selection) {
    final var application = selection.getEnrollmentApplication();
    final var person = application.getApplicantPerson();
    final var preference = application.getPreference();
    return new CourseWaitlistEntryResponse(
        selection.getId(),
        selection.getCourse().getId(),
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
