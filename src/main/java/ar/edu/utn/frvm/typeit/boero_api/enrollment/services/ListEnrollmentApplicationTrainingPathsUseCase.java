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
    final var application =
        enrollmentApplicationRepository
            .findById(applicationId)
            .filter(app -> app.getDeletedAt() == null)
            .filter(app -> app.getApplicantPerson().getId().equals(principal.personId()))
            .filter(app -> app.getInstitution().getId().equals(principal.institutionId()))
            .orElseThrow(EnrollmentApplicationNotFoundException::new);

    if (application.getEnrollmentPeriod() == null) {
      return List.of(TrainingPathResponse.from(application.getTrainingPath()));
    }
    return application.getEnrollmentPeriod().getOfferings().stream()
        .map(offering -> offering.getStudyPlan().getTrainingPath())
        .filter(path -> path.isActive() && path.getDeletedAt() == null)
        .distinct()
        .map(TrainingPathResponse::from)
        .toList();
  }
}
