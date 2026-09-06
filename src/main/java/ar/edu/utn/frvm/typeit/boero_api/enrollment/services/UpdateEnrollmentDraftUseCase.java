package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEnrollmentDraftUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId,
      final UUID personId,
      final UUID applicationId,
      final UpdateEnrollmentDraftRequest request) {
    final var application =
        enrollmentApplicationRepository
            .findByIdAndApplicantPersonIdAndInstitutionId(institutionId, personId, applicationId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);

    if (request.firstName() != null || request.lastName() != null) {
      final Person applicant = application.getApplicantPerson();
      applicant.updateIdentity(
          request.firstName() != null ? request.firstName() : applicant.getFirstName(),
          request.lastName() != null ? request.lastName() : applicant.getLastName(),
          applicant.getBirthDate(),
          applicant.getBirthCity(),
          applicant.getNationalityCountry());
    }
    if (request.secondarySchool() != null) {
      application.updateEducationBackground(request.secondarySchool());
    }
    return EnrollmentApplicationResponse.from(application);
  }
}
