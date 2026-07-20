package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import jakarta.validation.constraints.NotNull;

public record UpdatePersonAccessRequest(
    @NotNull(message = "El estado de acceso es requerido.") Boolean enabled) {}
