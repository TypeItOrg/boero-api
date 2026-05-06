package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LoginRequest(
    @NotBlank(message = "El número de documento es requerido.")
        @Pattern(
            regexp = "^[0-9]{8}$",
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @NotBlank(message = "La contraseña es requerida.")
        @Size(max = 255, message = "La contraseña debe tener menos de 255 carácteres.")
        String password,
    @NotNull(message = "La institución es requerida.") UUID institutionId,
    Boolean rememberMe) {}
