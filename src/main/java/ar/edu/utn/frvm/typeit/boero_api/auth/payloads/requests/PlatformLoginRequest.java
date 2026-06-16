package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformLoginRequest(
    @NotBlank(message = "El correo electrónico es requerido.")
        @Email(message = "El correo electrónico debe tener un formato válido.")
        String email,
    @NotBlank(message = "La contraseña es requerida.")
        @Size(max = PASSWORD_MAX, message = "La contraseña debe tener menos de 255 caracteres.")
        String password,
    Boolean rememberMe) {}
