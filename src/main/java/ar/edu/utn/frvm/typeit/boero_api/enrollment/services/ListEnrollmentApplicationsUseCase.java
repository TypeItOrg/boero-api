package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListEnrollmentApplicationsUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional(readOnly = true)
  public List<EnrollmentApplicationResponse> execute(final JwtAuthenticatedUser principal) {
    applicantEnrollmentGuard.requireApplicant(principal);
    return enrollmentApplicationRepository
        .findByPerson_IdAndInstitution_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            principal.personId(), principal.institutionId())
        .stream()
        .map(EnrollmentApplicationResponse::from)
        .toList();
  }
}
