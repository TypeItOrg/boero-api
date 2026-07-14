package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlatformAccountRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size(
            min = NAME_MIN,
            max = NAME_MAX,
            message = "El nombre debe tener entre 3 y 255 caracteres.")
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El nombre solo puede contener letras y espacios.")
        String name,
    @NotBlank(message = "El apellido es requerido.")
        @Size(
            min = NAME_MIN,
            max = NAME_MAX,
            message = "El apellido debe tener entre 3 y 255 caracteres.")
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @NotBlank(message = "El correo electrónico es requerido.")
        @Email(message = "El correo electrónico debe tener un formato válido.")
        @Size(max = 150, message = "El correo electrónico debe tener menos de 150 caracteres.")
        String email,
    @NotBlank(message = "La contraseña es requerida.")
        @Size(
            min = PASSWORD_MIN,
            max = PASSWORD_MAX,
            message = "La contraseña debe tener entre 8 y 255 caracteres.")
        String password) {}
