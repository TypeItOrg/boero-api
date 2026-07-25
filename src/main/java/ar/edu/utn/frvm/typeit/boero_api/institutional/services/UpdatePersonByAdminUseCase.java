package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionPersonResolver;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonByAdminRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePersonByAdminUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final PersonRepository personRepository;
  private final Validator validator;

  @Transactional
  public PersonResponse execute(
      final UUID institutionId, final UUID personId, final UpdatePersonByAdminRequest request) {
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    if (request.isEmpty()) {
      throw new ConstraintViolationException(Set.of());
    }

    person.updateIdentity(
        request.firstName() != null ? request.firstName() : person.getFirstName(),
        request.lastName() != null ? request.lastName() : person.getLastName(),
        person.getBirthDate(),
        person.getBirthCity(),
        person.getNationalityCountry());
    person.updateContact(
        request.email() != null ? request.email() : person.getEmail(),
        request.phoneNumber() != null ? request.phoneNumber() : person.getPhoneNumber());
    final var violations = validator.validate(person);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
    personRepository.save(person);

    return personRepository
        .findWithDetailsByIdAndInstitution_Id(personId, institutionId)
        .map(PersonResponse::from)
        .orElseThrow(PersonNotFoundException::new);
  }
}
