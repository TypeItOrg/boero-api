package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Student;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final StudentRepository studentRepository;
  private final PersonRepository personRepository;
  private final Clock clock;
  private final BusinessDateProvider businessDateProvider;
  private EnrollmentApplicationCourseApprovalService applicationCourseApprovalService;

  @Autowired(required = false)
  public void setApplicationCourseApprovalService(
      final EnrollmentApplicationCourseApprovalService applicationCourseApprovalService) {
    this.applicationCourseApprovalService = applicationCourseApprovalService;
  }

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId, final UUID applicationId, final UUID resolvedByPersonId) {
    final var currentStatus =
        enrollmentApplicationRepository.findStatusByInstitutionIdAndId(
            institutionId, applicationId);
    if (currentStatus.isPresent()
        && (currentStatus.get()
                == ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus
                    .APPROVED
            || currentStatus.get()
                == ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus
                    .REJECTED
            || currentStatus.get()
                == ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus
                    .CANCELLED)) {
      throw new InvalidEnrollmentApplicationStateException(
          EnrollmentMessages.APPLICATION_ALREADY_RESOLVED);
    }
    final var application =
        enrollmentApplicationRepository
            .findByIdAndInstitutionIdForUpdate(institutionId, applicationId)
            .orElseThrow(EnrollmentApplicationNotFoundException::new);

    personRepository
        .findByIdAndInstitutionIdForUpdate(application.getApplicantPerson().getId(), institutionId)
        .orElseThrow(EnrollmentApplicationNotFoundException::new);
    application.approve(clock.instant(), resolvedByPersonId);

    if (applicationCourseApprovalService != null && application.getStudyPlan() == null) {
      applicationCourseApprovalService.process(application);
    }

    // New course-based applications create Student only with the first effective enrollment.
    // The legacy plan-based flow retains its historical behavior until old clients migrate.
    final boolean legacyApplication =
        application.getStudyPlan() != null || application.hasLegacyStudyPlan();
    if (legacyApplication
        && !studentRepository.existsByInstitution_IdAndPerson_Id(
            institutionId, application.getApplicantPerson().getId())) {
      studentRepository.save(
          Student.builder()
              .institution(application.getInstitution())
              .person(application.getApplicantPerson())
              .fileNumber(generateFileNumber())
              .enrollmentDate(businessDateProvider.today())
              .build());
    }

    return EnrollmentApplicationResponse.from(application);
  }

  private String generateFileNumber() {
    final int year = businessDateProvider.today().getYear();
    final long sequence = studentRepository.nextFileNumberSequenceValue();

    return String.format("%d-%05d", year, sequence);
  }
}
