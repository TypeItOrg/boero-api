package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.UserAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RegisterRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserRegisteredResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

  private final UserRepository userRepository;
  private final InstitutionRepository institutionRepository;
  private final PersonRepository personRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserRegisteredResponse execute(RegisterRequest request) {
    Institution institution =
        institutionRepository
            .findById(request.institutionId())
            .orElseThrow(InstitutionNotFoundException::new);

    if (personRepository.existsByDocumentNumberAndInstitution_Id(
        request.documentNumber(), request.institutionId())) {
      throw new UserAlreadyExistsException();
    }

    Person person =
        Person.builder()
            .institution(institution)
            .firstName(request.name())
            .lastName(request.lastName())
            .documentNumber(request.documentNumber())
            .build();
    personRepository.save(person);

    User user =
        User.builder()
            .institution(institution)
            .person(person)
            .password(passwordEncoder.encode(request.password()))
            .build();
    userRepository.save(user);
    return new UserRegisteredResponse(user.getId(), user.getDocumentNumber(), user.getInstitutionId());
  }
}
