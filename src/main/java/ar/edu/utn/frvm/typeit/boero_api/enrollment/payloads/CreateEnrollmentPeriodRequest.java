package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateEnrollmentPeriodRequest(
    @NotNull(message = "El ciclo lectivo es obligatorio") UUID academicYearId,
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar los 150 caracteres")
        String name,
    @NotNull(message = "La fecha de inicio es obligatoria") LocalDateTime startDate,
    @NotNull(message = "La fecha de fin es obligatoria") LocalDateTime endDate) {}
