package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.CourseClass;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(requiredProperties = {"id", "teachers", "days"})
public record CourseClassResponse(
    UUID id, List<CourseTeacherResponse> teachers, List<CourseClassDayResponse> days) {

  public static CourseClassResponse from(
      final CourseClass courseClass,
      final List<CourseTeacherResponse> teachers,
      final List<CourseClassDayResponse> days) {
    return new CourseClassResponse(courseClass.getId(), teachers, days);
  }
}
