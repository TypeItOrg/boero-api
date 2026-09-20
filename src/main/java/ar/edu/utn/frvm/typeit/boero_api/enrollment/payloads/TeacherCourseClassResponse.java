package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseClassResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "courseId",
      "academicSpaceName",
      "instrumentName",
      "classLabel",
      "courseClass"
    })
public record TeacherCourseClassResponse(
    UUID courseId,
    String academicSpaceName,
    @Schema(nullable = true) String instrumentName,
    String classLabel,
    CourseClassResponse courseClass) {}
