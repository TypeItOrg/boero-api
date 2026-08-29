package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEnrollmentApplicationUseCase {

  private final ApplicantEnrollmentGuard applicantEnrollmentGuard;
  private final StudyPlanRepository studyPlanRepository;
  private final AcademicYearRepository academicYearRepository;
  private final EnrollmentApplicationRepository enrollmentApplicationRepository;

  @Transactional
  public EnrollmentApplicationResponse execute(
      final JwtAuthenticatedUser principal, final CreateEnrollmentApplicationRequest request) {
    final var applicant = applicantEnrollmentGuard.requireApplicant(principal);
    final var studyPlan =
        studyPlanRepository
            .findByIdAndInstitution_Id(request.studyPlanId(), principal.institutionId())
            .orElseThrow(StudyPlanNotFoundException::new);
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_Id(request.academicYearId(), principal.institutionId())
            .orElseThrow(AcademicYearNotFoundException::new);
    final EnrollmentApplication application =
        EnrollmentApplication.create(
            applicant, applicant.getInstitution(), studyPlan, academicYear, null);
    return EnrollmentApplicationResponse.from(enrollmentApplicationRepository.save(application));
  }
}
