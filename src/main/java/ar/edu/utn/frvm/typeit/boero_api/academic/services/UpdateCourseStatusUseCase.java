package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCourseStatusUseCase {
  private final CourseRepository courseRepository;
  private final AcademicYearRepository academicYearRepository;
  private final StudyPlanRepository studyPlanRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final CourseStatusRequest request) {
    final var context =
        courseRepository
            .findAcademicContextByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    final var studyPlan =
        studyPlanRepository
            .findByIdAndInstitution_IdForUpdate(context.getStudyPlanId(), institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_IdForUpdate(context.getAcademicYearId(), institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    final var course =
        courseRepository
            .findByIdAndInstitution_IdForUpdate(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (request.status() == CourseStatus.ACTIVE) {
      if (studyPlan.getStatus() != StudyPlanStatus.ACTIVE) {
        throw new AcademicConflictException(AcademicMessages.COURSE_STUDY_PLAN_NOT_ACTIVE);
      }
      if (academicYear.getStatus() != AcademicYearStatus.ACTIVE) {
        throw new AcademicConflictException(AcademicMessages.COURSE_YEAR_NOT_ACTIVE);
      }
    }
    course.updateStatus(request.status());
    courseRepository.flush();
  }
}
