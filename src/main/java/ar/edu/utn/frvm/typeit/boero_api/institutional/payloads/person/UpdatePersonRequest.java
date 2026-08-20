package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.MINIMUM_AGE;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.MinimumAge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePersonRequest(
    @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres.")
        @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El nombre solo puede contener letras y espacios.")
        String firstName,
    @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres.")
        @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @MinimumAge(MINIMUM_AGE) LocalDate birthDate,
    @Email(message = "El correo electrónico debe tener un formato válido.")
        @Size(max = 150, message = "El correo electrónico debe tener menos de 150 caracteres.")
        String email,
    @Size(max = 30, message = "El teléfono debe tener menos de 30 caracteres.") String phoneNumber,
    UUID birthCityId,
    UUID nationalityCountryId,
    @Valid UpdateAddressRequest address,
    @Size(max = PASSWORD_MAX, message = "La contraseña actual no puede superar los 255 caracteres.")
        String currentPassword,
    @Pattern(
            regexp = "^$|(?s:.{" + PASSWORD_MIN + "," + PASSWORD_MAX + "})$",
            message = "La contraseña debe tener entre 8 y 255 caracteres.")
        String password) {

  public UpdatePersonRequest(
      String firstName,
      String lastName,
      LocalDate birthDate,
      String email,
      String phoneNumber,
      UUID birthCityId,
      UUID nationalityCountryId,
      UpdateAddressRequest address) {
    this(
        firstName,
        lastName,
        birthDate,
        email,
        phoneNumber,
        birthCityId,
        nationalityCountryId,
        address,
        null,
        null);
  }

  public UpdatePersonRequest(
      String firstName,
      String lastName,
      LocalDate birthDate,
      String email,
      String phoneNumber,
      UUID birthCityId,
      UUID nationalityCountryId,
      UpdateAddressRequest address,
      String password) {
    this(
        firstName,
        lastName,
        birthDate,
        email,
        phoneNumber,
        birthCityId,
        nationalityCountryId,
        address,
        null,
        password);
  }
}
