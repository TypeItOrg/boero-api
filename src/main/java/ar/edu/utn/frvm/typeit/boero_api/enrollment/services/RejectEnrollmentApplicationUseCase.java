package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.RejectEnrollmentApplicationRequest;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final Clock clock;
  private EnrollmentApplicationCourseRepository applicationCourseRepository;

  @Autowired(required = false)
  public void setApplicationCourseRepository(
      final EnrollmentApplicationCourseRepository applicationCourseRepository) {
    this.applicationCourseRepository = applicationCourseRepository;
  }

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId,
      final UUID applicationId,
      final RejectEnrollmentApplicationRequest request,
      final UUID resolvedByPersonId) {
    final var application =
        enrollmentApplicationRepository
            .findByIdAndInstitutionIdForUpdate(institutionId, applicationId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);
    application.reject(request.rejectionReason(), clock.instant(), resolvedByPersonId);
    if (applicationCourseRepository != null) {
      applicationCourseRepository
          .findByApplicationIdAndStatuses(
              applicationId, List.of(EnrollmentApplicationCourseStatus.PENDING))
          .forEach(
              selection ->
                  selection.reject(
                      "DOCUMENTATION_REJECTED",
                      request.rejectionReason(),
                      clock.instant(),
                      resolvedByPersonId));
    }

    return EnrollmentApplicationResponse.from(application);
  }
}
