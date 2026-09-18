package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassDay;
import java.util.List;
import java.util.UUID;

public record CourseEnrollmentDayOptionResponse(
    UUID id,
    String dayOfWeek,
    Integer capacity,
    Integer periodDurationMinutes,
    List<CourseEnrollmentScheduleOptionResponse> schedules) {

  public static CourseEnrollmentDayOptionResponse from(
      final CourseClassDay day, final List<CourseEnrollmentScheduleOptionResponse> schedules) {
    return new CourseEnrollmentDayOptionResponse(
        day.getId(),
        day.getDayOfWeek().name(),
        day.getCapacity(),
        day.getPeriodDurationMinutes(),
        schedules);
  }
}
