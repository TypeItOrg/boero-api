package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentHistory;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.AcademicEnrollmentStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.CourseEnrollmentStatus;
import java.time.Instant;
import java.util.UUID;

public record CourseEnrollmentHistoryResponse(
    UUID id,
    CourseEnrollmentStatus previousStatus,
    CourseEnrollmentStatus newStatus,
    AcademicEnrollmentStatus previousAcademicStatus,
    AcademicEnrollmentStatus newAcademicStatus,
    String operation,
    String reason,
    UUID authorityPersonId,
    Instant changedAt) {

  public static CourseEnrollmentHistoryResponse from(final CourseEnrollmentHistory history) {
    return new CourseEnrollmentHistoryResponse(
        history.getId(),
        history.getPreviousStatus(),
        history.getNewStatus(),
        history.getPreviousAcademicStatus(),
        history.getNewAcademicStatus(),
        history.getOperation(),
        history.getReason(),
        history.getAuthorityPersonId(),
        history.getChangedAt());
  }
}
