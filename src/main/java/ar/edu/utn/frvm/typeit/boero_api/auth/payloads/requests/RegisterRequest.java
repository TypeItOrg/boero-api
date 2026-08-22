package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.MINIMUM_AGE;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.MinimumAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size.List({
          @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El nombre solo puede contener letras y espacios.")
        String name,
    @NotBlank(message = "El apellido es requerido.")
        @Size.List({
          @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @NotNull(message = "La fecha de nacimiento es requerida.") @MinimumAge(MINIMUM_AGE)
        LocalDate birthDate,
    @NotNull(message = "El número de documento es requerido.")
        @Pattern(
            regexp = DOCUMENT_PATTERN,
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @NotBlank(message = "El email es requerido.")
        @Email(message = "El email debe tener un formato válido.")
        @Size(max = 150, message = "El email debe tener menos de 150 caracteres.")
        String email,
    @NotNull(message = "La contraseña es requerida.")
        @Size.List({
          @Size(min = PASSWORD_MIN, message = "La contraseña debe tener al menos 8 caracteres."),
          @Size(max = PASSWORD_MAX, message = "La contraseña debe tener menos de 255 caracteres.")
        })
        String password,
    @NotNull(message = "La institución es requerida.") UUID institutionId) {}
