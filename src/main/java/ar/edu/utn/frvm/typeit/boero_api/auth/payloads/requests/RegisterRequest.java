package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size.List({
          @Size(min = 3, message = "El nombre debe tener al menos 3 carácteres."),
          @Size(max = 255, message = "El nombre debe tener menos de 255 carácteres.")
        })
        @Pattern(
            regexp = "^[\\p{L} ]+$",
            message = "El nombre solo puede contener letras y espacios.")
        String name,
    @NotBlank(message = "El apellido es requerido.")
        @Size.List({
          @Size(min = 3, message = "El apellido debe tener al menos 3 carácteres."),
          @Size(max = 255, message = "El apellido debe tener menos de 255 carácteres.")
        })
        @Pattern(
            regexp = "^[\\p{L} ]+$",
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @NotNull(message = "El número de documento es requerido.")
        @Pattern(
            regexp = "^[0-9]{8}$",
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @NotNull(message = "La contraseña es requerida.")
        @Size.List({
          @Size(min = 8, message = "La contraseña debe tener al menos 8 carácteres."),
          @Size(max = 255, message = "La contraseña debe tener menos de 255 carácteres.")
        })
        String password,
    @NotNull(message = "La institución es requerida.") UUID institutionId) {}
