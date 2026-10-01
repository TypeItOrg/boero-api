package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(requiredProperties = {"person", "enabled", "function", "courses"})
public record TeacherDetailResponse(
    PersonResponse person,
    boolean enabled,
    String function,
    List<CourseAssignmentResponse> courses) {

  public record CourseAssignmentResponse(
      UUID courseId,
      String academicSpaceName,
      @Schema(nullable = true) @Nullable String instrumentName,
      int academicYear,
      List<CourseClassResponse> classes) {}
}
