package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "El token de recuperación es requerido.") String token,
    @NotBlank(message = "La contraseña es requerida.")
        @Size(
            min = PASSWORD_MIN,
            max = PASSWORD_MAX,
            message = "La contraseña debe tener entre 8 y 255 caracteres.")
        String password,
    @NotBlank(message = "La confirmación de la contraseña es requerida.")
        @Size(
            min = PASSWORD_MIN,
            max = PASSWORD_MAX,
            message = "La confirmación de la contraseña debe tener entre 8 y 255 caracteres.")
        String confirmPassword) {}
