package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.PASSWORD_MIN;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePersonByAdminRequest(
    @Size.List({
          @Size(min = NAME_MIN, message = ValidationMessages.FIRST_NAME_MIN_LENGTH),
          @Size(max = NAME_MAX, message = ValidationMessages.FIRST_NAME_MAX_LENGTH)
        })
        @Pattern(regexp = NAME_PATTERN, message = ValidationMessages.FIRST_NAME_FORMAT)
        String firstName,
    @Size.List({
          @Size(min = NAME_MIN, message = ValidationMessages.LAST_NAME_MIN_LENGTH),
          @Size(max = NAME_MAX, message = ValidationMessages.LAST_NAME_MAX_LENGTH)
        })
        @Pattern(regexp = NAME_PATTERN, message = ValidationMessages.LAST_NAME_FORMAT)
        String lastName,
    @Email(message = ValidationMessages.PERSON_EMAIL_FORMAT) String email,
    String phoneNumber,
    @Pattern(
            regexp = "^$|(?s:.{" + PASSWORD_MIN + "," + PASSWORD_MAX + "})$",
            message = ValidationMessages.PASSWORD_RANGE)
        String password) {

  public UpdatePersonByAdminRequest(
      final String firstName, final String lastName, final String email, final String phoneNumber) {
    this(firstName, lastName, email, phoneNumber, null);
  }

  public boolean hasPassword() {
    return password != null && !password.isEmpty();
  }

  public boolean isEmpty() {
    return firstName == null
        && lastName == null
        && email == null
        && phoneNumber == null
        && !hasPassword();
  }
}
