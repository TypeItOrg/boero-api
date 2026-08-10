package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTrainingPathRequest(
    @NotBlank @Size(max = 150) String name, @Size(max = 1000) String description) {}
