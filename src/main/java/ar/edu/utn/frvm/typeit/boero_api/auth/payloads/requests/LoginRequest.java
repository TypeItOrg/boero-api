package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LoginRequest(
    @NotBlank(message = "El número de documento es requerido.")
        @Pattern(
            regexp = DOCUMENT_PATTERN,
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @NotBlank(message = "La contraseña es requerida.")
        @Size(max = PASSWORD_MAX, message = "La contraseña debe tener menos de 255 caracteres.")
        String password,
    @NotNull(message = "La institución es requerida.") UUID institutionId,
    Boolean rememberMe) {}
