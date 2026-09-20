package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationCourse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationCourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationCourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationCourseResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.RejectEnrollmentApplicationCourseRequest;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectEnrollmentApplicationCourseUseCase {

  private final EnrollmentApplicationCourseRepository applicationCourseRepository;
  private final Clock clock;

  private final EnrollmentInstitutionLock enrollmentInstitutionLock;

  @Transactional
  public EnrollmentApplicationCourseResponse execute(
      final UUID institutionId,
      final UUID applicationId,
      final UUID applicationCourseId,
      final RejectEnrollmentApplicationCourseRequest request,
      final UUID resolvedByPersonId) {
    enrollmentInstitutionLock.lock(institutionId);
    final EnrollmentApplicationCourse selection =
        applicationCourseRepository
            .findByIdAndInstitutionIdForUpdate(applicationCourseId, institutionId)
            .orElseThrow(
                () ->
                    new EnrollmentValidationException(
                        EnrollmentMessages.COURSE_APPLICATION_NOT_FOUND));
    if (!selection.getEnrollmentApplication().getId().equals(applicationId)) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_APPLICATION_NOT_FOUND);
    }
    if (!selection.getEnrollmentApplication().isApproved()) {
      throw new EnrollmentValidationException(EnrollmentMessages.PARENT_NOT_APPROVED);
    }
    if (request.expectedVersion() != null && request.expectedVersion() != selection.getVersion()) {
      throw new EnrollmentValidationException(EnrollmentMessages.COURSE_ENROLLMENT_VERSION_STALE);
    }
    if (selection.getStatus() != EnrollmentApplicationCourseStatus.PENDING
        && selection.getStatus() != EnrollmentApplicationCourseStatus.WAITLISTED) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.COURSE_APPLICATION_ALREADY_RESOLVED);
    }

    selection.reject("ADMIN_REJECTED", request.reason(), clock.instant(), resolvedByPersonId);
    return EnrollmentApplicationCourseResponse.from(selection);
  }
}
