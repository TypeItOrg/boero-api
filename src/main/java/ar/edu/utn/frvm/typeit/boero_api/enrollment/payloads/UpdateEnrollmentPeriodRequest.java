package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateEnrollmentPeriodRequest(
    @NotBlank(message = EnrollmentMessages.NAME_REQUIRED)
        @Size(max = 150, message = EnrollmentMessages.NAME_TOO_LONG)
        String name,
    @NotNull(message = EnrollmentMessages.START_DATE_REQUIRED) Instant startDate,
    @NotNull(message = EnrollmentMessages.END_DATE_REQUIRED) Instant endDate) {}
