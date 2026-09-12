package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicantEnrollmentGuard {

  private final PersonRepository personRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;

  public Person requireApplicant(final JwtAuthenticatedUser principal) {
    final boolean isApplicant =
        personRoleAssignmentRepository.existsByPerson_IdAndInstitution_IdAndRole_Code(
            principal.personId(), principal.institutionId(), SystemRoleCode.APPLICANT.name());
    if (!isApplicant) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
    return personRepository
        .findByIdAndInstitution_Id(principal.personId(), principal.institutionId())
        .orElseThrow(PersonNotFoundException::new);
  }
}
