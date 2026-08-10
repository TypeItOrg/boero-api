package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAcademicLevelRequest(
    @NotBlank @Size(max = 150) String name,
    @Min(value = 1, message = "El orden debe ser positivo.") int displayOrder,
    @Size(max = 1000) String description) {}
