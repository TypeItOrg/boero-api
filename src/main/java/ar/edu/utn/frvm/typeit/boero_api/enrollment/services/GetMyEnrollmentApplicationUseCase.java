package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse execute(
      final UUID institutionId, final UUID personId, final UUID applicationId) {
    return enrollmentApplicationRepository
        .findByIdAndApplicantPersonIdAndInstitutionId(institutionId, personId, applicationId)
        .map(EnrollmentApplicationResponse::from)
        .orElseThrow(EnrollmentApplicationNotFoundException::new);
  }
}
