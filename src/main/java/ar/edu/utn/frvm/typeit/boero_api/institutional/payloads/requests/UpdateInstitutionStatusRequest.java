package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotNull;

public record UpdateInstitutionStatusRequest(
    @NotNull(message = ValidationMessages.ACTIVE_REQUIRED) Boolean active) {}
