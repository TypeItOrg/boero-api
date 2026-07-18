package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.MINIMUM_AGE;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.common.validation.MinimumAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePersonRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size.List({
          @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El nombre solo puede contener letras y espacios.")
        String firstName,
    @NotBlank(message = "El apellido es requerido.")
        @Size.List({
          @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @NotBlank(message = "El número de documento es requerido.")
        @Pattern(
            regexp = DOCUMENT_PATTERN,
            message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
        String documentNumber,
    @Email(message = "El email debe tener un formato válido.") String email,
    String phoneNumber,
    @NotNull(message = "La fecha de nacimiento es requerida.") @MinimumAge(MINIMUM_AGE)
        LocalDate birthDate,
    @NotBlank(message = "La contraseña es requerida.")
        @Size.List({
          @Size(min = PASSWORD_MIN, message = "La contraseña debe tener al menos 8 caracteres."),
          @Size(max = PASSWORD_MAX, message = "La contraseña debe tener menos de 255 caracteres.")
        })
        String password,
    SystemRoleCode initialRole) {}
