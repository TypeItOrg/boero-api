package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_PATTERN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record PasswordRecoveryRequest(
    @NotBlank(message = "El número de documento es requerido.")
        @Pattern(
            regexp = DOCUMENT_PATTERN,
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @NotNull(message = "La institución es requerida.") UUID institutionId) {}
