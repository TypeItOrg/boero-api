package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentPeriodResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListEnrollmentApplicationPeriodsUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentPeriodRepository enrollmentPeriodRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public PaginatedResponse<EnrollmentPeriodResponse> execute(
      final JwtAuthenticatedUser principal, final Pageable pageable) {
    applicantEnrollmentGuard.requireApplicant(principal);

    return PaginatedResponse.from(
        enrollmentPeriodRepository
            .findAvailableForEnrollment(
                principal.institutionId(), EnrollmentPeriodStatus.OPEN, clock.instant(), pageable)
            .map(EnrollmentPeriodResponse::from));
  }
}
