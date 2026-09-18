package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateCourseRequest(
    UUID studyPlanSpaceId,
    UUID instrumentId,
    UUID studyPlanId,
    UUID academicSpaceId,
    @NotNull UUID academicYearId,
    @NotEmpty @Valid List<@NotNull @Valid CourseClassRequest> classes) {

  public CreateCourseRequest(
      final UUID studyPlanId,
      final UUID academicSpaceId,
      final UUID academicYearId,
      final List<CourseClassRequest> classes) {
    this(null, null, studyPlanId, academicSpaceId, academicYearId, classes);
  }
}
