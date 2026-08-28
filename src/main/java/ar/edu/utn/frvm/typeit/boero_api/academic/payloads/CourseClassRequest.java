package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CourseClassRequest(
    @NotEmpty List<UUID> teacherIds, @NotEmpty @Valid List<CourseClassDayRequest> days) {}
