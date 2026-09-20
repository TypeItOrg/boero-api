package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClassDay;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "dayOfWeek",
      "capacity",
      "availableCapacity",
      "periodDurationMinutes",
      "schedules"
    })
public record CourseEnrollmentDayOptionResponse(
    UUID id,
    String dayOfWeek,
    @Schema(nullable = true) Integer capacity,
    @Schema(nullable = true) Integer availableCapacity,
    @Schema(nullable = true) Integer periodDurationMinutes,
    List<CourseEnrollmentScheduleOptionResponse> schedules) {

  public static CourseEnrollmentDayOptionResponse from(
      final CourseClassDay day,
      final List<CourseEnrollmentScheduleOptionResponse> schedules,
      final long occupied) {
    return new CourseEnrollmentDayOptionResponse(
        day.getId(),
        day.getDayOfWeek().name(),
        day.getCapacity(),
        day.getCapacity() == null ? null : Math.max(0, day.getCapacity() - (int) occupied),
        day.getPeriodDurationMinutes(),
        schedules);
  }
}
