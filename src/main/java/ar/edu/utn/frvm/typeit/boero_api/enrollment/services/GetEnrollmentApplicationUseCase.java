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
public class GetEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional(readOnly = true)
  public EnrollmentApplicationResponse execute(final UUID institutionId, final UUID applicationId) {
    // Course selections are always included: both callers (institutional and platform
    // detail) already enforce their review permissions, and reviewers need the
    // selections before approval to enroll from them.
    return enrollmentApplicationRepository
        .findByIdAndInstitutionId(institutionId, applicationId)
        .map(application -> EnrollmentApplicationResponse.from(application, true))
        .orElseThrow(EnrollmentApplicationNotFoundException::new);
  }
}
