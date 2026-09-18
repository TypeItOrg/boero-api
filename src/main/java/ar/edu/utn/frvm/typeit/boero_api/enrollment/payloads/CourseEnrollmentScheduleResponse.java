package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.CourseEnrollmentSchedule;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record CourseEnrollmentScheduleResponse(
    UUID id,
    UUID classScheduleId,
    UUID individualSlotId,
    String dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Instant releasedAt) {

  public static CourseEnrollmentScheduleResponse from(final CourseEnrollmentSchedule schedule) {
    return new CourseEnrollmentScheduleResponse(
        schedule.getId(),
        schedule.getSchedule().getId(),
        schedule.getIndividualSlot() == null ? null : schedule.getIndividualSlot().getId(),
        schedule.getDayOfWeek().name(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.getReleasedAt());
  }
}
