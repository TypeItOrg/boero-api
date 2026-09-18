package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAvailableEnrollmentTrainingPathsUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final TrainingPathRepository trainingPathRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<TrainingPathResponse> execute(
      final JwtAuthenticatedUser principal, final Pageable pageable) {
    applicantEnrollmentGuard.requireApplicant(principal);

    final UUID institutionId = principal.institutionId();

    return PaginatedResponse.from(
        trainingPathRepository
            .findAvailableForEnrollment(institutionId, pageable)
            .map(TrainingPathResponse::from));
  }
}
