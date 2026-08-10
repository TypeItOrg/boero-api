package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAcademicSpaceRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @NotNull AcademicSpaceType type) {}
