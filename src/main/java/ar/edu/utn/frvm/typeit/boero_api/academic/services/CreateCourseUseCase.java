package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateCourseRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCourseUseCase {
  private final InstitutionRepository institutionRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final AcademicSpaceRepository academicSpaceRepository;
  private final AcademicYearRepository academicYearRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final CourseRepository courseRepository;
  private final CourseClassAssembler courseClassAssembler;
  private final CourseTreeReader courseTreeReader;

  @Transactional
  public CourseResponse execute(final UUID institutionId, final CreateCourseRequest request) {
    final var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForUpdate(request.studyPlanId(), institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    if (plan.getStatus() != StudyPlanStatus.ACTIVE) {
      throw new AcademicConflictException(AcademicMessages.COURSE_STUDY_PLAN_NOT_ACTIVE);
    }
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdForUpdate(request.academicSpaceId(), institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    if (!studyPlanSpaceRepository.existsByStudyPlan_IdAndAcademicSpace_Id(
        plan.getId(), space.getId())) {
      throw new AcademicConflictException(AcademicMessages.COURSE_SPACE_NOT_IN_PLAN);
    }
    final var year =
        academicYearRepository
            .findByIdAndInstitution_IdForUpdate(request.academicYearId(), institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    if (year.getStatus() != AcademicYearStatus.ACTIVE) {
      throw new AcademicConflictException(AcademicMessages.COURSE_YEAR_NOT_ACTIVE);
    }
    if (courseRepository.existsByInstitutionAndSpaceAndYear(
        institutionId, space.getId(), year.getId())) {
      throw new AcademicConflictException(AcademicMessages.COURSE_ALREADY_EXISTS);
    }
    try {
      final var course = courseRepository.save(Course.create(institution, plan, space, year));
      courseClassAssembler.assemble(institution, course, space.getFormat(), request.classes());
      courseRepository.flush();
      return CourseResponse.from(course, courseTreeReader.read(course.getId()));
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
