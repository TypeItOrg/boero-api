package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReplaceCourseClassesRequest(@NotEmpty @Valid List<CourseClassRequest> classes) {}
