package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StartEnrollmentApplicationUseCase {

  private final EnrollmentApplicationRepository enrollmentApplicationRepository;
  private final EnrollmentPeriodRepository enrollmentPeriodRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final AcademicYearRepository academicYearRepository;
  private final InstitutionRepository institutionRepository;
  private final PersonRepository personRepository;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final UUID institutionId,
      final UUID personId,
      final StartEnrollmentApplicationRequest request) {
    return enrollmentApplicationRepository
        .findByInstitutionIdAndApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatus(
            institutionId,
            personId,
            request.studyPlanId(),
            request.academicYearId(),
            EnrollmentApplicationStatus.DRAFT)
        .map(EnrollmentApplicationResponse::from)
        .orElseGet(
            () -> {
              final Institution institution =
                  institutionRepository
                      .findById(institutionId)
                      .orElseThrow(InstitutionNotFoundException::new);
              final Person applicant =
                  personRepository
                      .findByIdAndInstitution_Id(personId, institutionId)
                      .orElseThrow(PersonNotFoundException::new);
              final StudyPlan studyPlan =
                  studyPlanRepository
                      .findByIdAndInstitution_Id(request.studyPlanId(), institutionId)
                      .orElseThrow(StudyPlanNotFoundException::new);
              final AcademicYear academicYear =
                  academicYearRepository
                      .findByIdAndInstitution_Id(request.academicYearId(), institutionId)
                      .orElseThrow(AcademicYearNotFoundException::new);
              final EnrollmentPeriod enrollmentPeriod =
                  enrollmentPeriodRepository
                      .findByInstitutionIdAndAcademicYearIdAndStatusForUpdate(
                          institutionId, request.academicYearId(), EnrollmentPeriodStatus.OPEN)
                      .orElseThrow(EnrollmentPeriodClosedException::new);
              final EnrollmentApplication application =
                  EnrollmentApplication.create(
                      institution, applicant, studyPlan, academicYear, enrollmentPeriod);
              return EnrollmentApplicationResponse.from(
                  enrollmentApplicationRepository.save(application));
            });
  }
}
