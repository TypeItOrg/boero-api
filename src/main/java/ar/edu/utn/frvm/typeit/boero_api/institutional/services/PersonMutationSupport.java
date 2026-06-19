package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonByAdminRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;

final class PersonMutationSupport {

  static void applyBasicFields(
      final Person person, final UpdatePersonByAdminRequest request, final Validator validator) {

    if (request.isEmpty()) throw new ConstraintViolationException(Set.of());

    if (request.firstName() != null) {
      person.setFirstName(request.firstName());
    }

    if (request.lastName() != null) {
      person.setLastName(request.lastName());
    }

    if (request.email() != null) {
      person.setEmail(request.email());
    }

    if (request.phoneNumber() != null) {
      person.setPhoneNumber(request.phoneNumber());
    }

    assertValid(person, validator);
  }

  static void assertValid(final Person person, final Validator validator) {
    Set<ConstraintViolation<Person>> violations = validator.validate(person);
    if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
  }
}
