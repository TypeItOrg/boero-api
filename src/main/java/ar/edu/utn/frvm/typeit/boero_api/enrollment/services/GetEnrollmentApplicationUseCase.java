package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEnrollmentApplicationUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse execute(
      final JwtAuthenticatedUser principal, final UUID applicationId) {
    applicantEnrollmentGuard.requireApplicant(principal);
    return enrollmentApplicationRepository
        .findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
            applicationId, principal.personId(), principal.institutionId())
        .map(EnrollmentApplicationResponse::from)
        .orElseThrow(EnrollmentApplicationNotFoundException::new);
  }
}
