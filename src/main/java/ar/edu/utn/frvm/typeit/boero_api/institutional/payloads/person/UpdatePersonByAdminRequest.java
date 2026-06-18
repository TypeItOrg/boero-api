package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePersonByAdminRequest(
    @Size.List({
          @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El nombre solo puede contener letras y espacios.")
        String firstName,
    @Size.List({
          @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres."),
          @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
        })
        @Pattern(
            regexp = NAME_PATTERN,
            message = "El apellido solo puede contener letras y espacios.")
        String lastName,
    @Email(message = "El email debe tener un formato válido.") String email,
    String phoneNumber) {

  public boolean isEmpty() {
    return firstName == null && lastName == null && email == null && phoneNumber == null;
  }
}
