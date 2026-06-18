package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AssignPersonSystemRoleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.CreatePersonRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import jakarta.validation.Validator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePersonUseCase {

  private final InstitutionRepository institutionRepository;
  private final PersonRepository personRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Validator validator;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @Transactional
  public PersonResponse execute(final UUID institutionId, final CreatePersonRequest request) {
    Institution institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    if (personRepository.existsByDocumentNumberAndInstitution_Id(
        request.documentNumber(), institutionId)) {
      throw new PersonAlreadyExistsException();
    }

    Person person =
        Person.builder()
            .institution(institution)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .documentNumber(request.documentNumber())
            .email(request.email())
            .phoneNumber(request.phoneNumber())
            .birthDate(request.birthDate())
            .build();
    PersonMutationSupport.assertValid(person, validator);
    personRepository.save(person);

    User user =
        User.builder()
            .institution(institution)
            .person(person)
            .password(passwordEncoder.encode(request.password()))
            .build();
    userRepository.save(user);

    SystemRoleCode role =
        request.initialRole() != null ? request.initialRole() : SystemRoleCode.APPLICANT;
    assignPersonSystemRoleUseCase.execute(person, role);
    return PersonResponse.from(person);
  }
}
