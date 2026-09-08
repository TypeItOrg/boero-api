package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentPeriodStatusRequest(
    @NotNull(message = "El estado es obligatorio") EnrollmentPeriodStatus status) {}
