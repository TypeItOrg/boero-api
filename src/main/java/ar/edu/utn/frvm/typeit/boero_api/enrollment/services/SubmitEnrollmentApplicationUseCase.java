package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId, final UUID personId, final UUID applicationId) {
    final var application =
        enrollmentApplicationRepository
            .findByIdAndApplicantPersonIdAndInstitutionId(institutionId, personId, applicationId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    application.submit();
    return EnrollmentApplicationResponse.from(application);
  }
}
