package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record CourseStatusRequest(@NotNull CourseStatus status) {}
