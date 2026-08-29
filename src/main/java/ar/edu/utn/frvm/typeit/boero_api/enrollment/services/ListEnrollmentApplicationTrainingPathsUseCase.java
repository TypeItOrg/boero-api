package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListEnrollmentApplicationTrainingPathsUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final TrainingPathRepository trainingPathRepository;

  @Transactional(readOnly = true)
  public List<TrainingPathResponse> execute(
      final JwtAuthenticatedUser principal, final UUID applicationId) {
    applicantEnrollmentGuard.requireApplicant(principal);
    enrollmentApplicationRepository
        .findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
            applicationId, principal.personId(), principal.institutionId())
        .orElseThrow(EnrollmentApplicationNotFoundException::new);
    return trainingPathRepository
        .findByInstitution_IdAndActiveTrueAndDeletedAtIsNullOrderByNameAsc(
            principal.institutionId())
        .stream()
        .map(TrainingPathResponse::from)
        .toList();
  }
}
