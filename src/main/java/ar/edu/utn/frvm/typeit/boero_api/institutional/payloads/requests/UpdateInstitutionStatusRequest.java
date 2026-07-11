package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests;

import jakarta.validation.constraints.NotNull;

public record UpdateInstitutionStatusRequest(
    @NotNull(message = "El estado activo es requerido.") Boolean active) {}
