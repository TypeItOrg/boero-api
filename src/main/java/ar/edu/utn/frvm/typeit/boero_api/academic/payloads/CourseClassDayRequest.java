package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseDay;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CourseClassDayRequest(
    @NotNull CourseDay dayOfWeek,
    @Positive Integer capacity,
    @Positive Integer periodDurationMinutes,
    @NotEmpty @Valid List<@NotNull @Valid CourseClassScheduleRequest> schedules) {}
