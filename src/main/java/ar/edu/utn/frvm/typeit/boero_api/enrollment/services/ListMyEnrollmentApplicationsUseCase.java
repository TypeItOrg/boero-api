package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListMyEnrollmentApplicationsUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional(readOnly = true)
  public Page<EnrollmentApplicationResponse> execute(
      final UUID institutionId,
      final UUID personId,
      @Nullable final EnrollmentApplicationStatus status,
      final Pageable pageable) {
    return enrollmentApplicationRepository
        .findMyApplications(institutionId, personId, status, pageable)
        .map(EnrollmentApplicationResponse::from);
  }
}
