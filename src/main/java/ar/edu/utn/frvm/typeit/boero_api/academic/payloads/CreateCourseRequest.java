package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateCourseRequest(
    @NotNull UUID studyPlanId,
    @NotNull UUID academicSpaceId,
    @NotNull UUID academicYearId,
    @NotEmpty @Valid List<CourseClassRequest> classes) {}
