package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseStatusRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCourseStatusUseCaseTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID ACADEMIC_YEAR_ID = UUID.randomUUID();
  private static final UUID STUDY_PLAN_ID = UUID.randomUUID();

  @Mock private CourseRepository courseRepository;
  @Mock private AcademicYearRepository academicYearRepository;
  @Mock private AcademicYear academicYear;
  @Mock private Course course;
  @Mock private StudyPlan studyPlan;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private CourseRepository.CourseAcademicContext courseAcademicContext;

  @Test
  void rejectsActivatingCourseWhenItsAcademicYearIsClosed() {
    stubCourseContext();
    given(studyPlan.getStatus()).willReturn(StudyPlanStatus.ACTIVE);
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(STUDY_PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(studyPlan));
    given(
            academicYearRepository.findByIdAndInstitution_IdForUpdate(
                ACADEMIC_YEAR_ID, INSTITUTION_ID))
        .willReturn(Optional.of(academicYear));
    given(academicYear.getStatus()).willReturn(AcademicYearStatus.CLOSED);
    given(courseRepository.findByIdAndInstitution_IdForUpdate(COURSE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(course));

    final var useCase =
        new UpdateCourseStatusUseCase(
            courseRepository, academicYearRepository, studyPlanRepository);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    INSTITUTION_ID, COURSE_ID, new CourseStatusRequest(CourseStatus.ACTIVE)))
        .isInstanceOf(AcademicConflictException.class);

    verify(course, never()).updateStatus(any());
    verify(courseRepository, never()).flush();
  }

  @Test
  void rejectsActivatingCourseWhenItsStudyPlanIsInactive() {
    stubCourseContext();
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(STUDY_PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(studyPlan));
    given(studyPlan.getStatus()).willReturn(StudyPlanStatus.INACTIVE);
    given(
            academicYearRepository.findByIdAndInstitution_IdForUpdate(
                ACADEMIC_YEAR_ID, INSTITUTION_ID))
        .willReturn(Optional.of(academicYear));
    given(courseRepository.findByIdAndInstitution_IdForUpdate(COURSE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(course));

    final var useCase =
        new UpdateCourseStatusUseCase(
            courseRepository, academicYearRepository, studyPlanRepository);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    INSTITUTION_ID, COURSE_ID, new CourseStatusRequest(CourseStatus.ACTIVE)))
        .isInstanceOf(AcademicConflictException.class);

    verify(course, never()).updateStatus(any());
    verify(courseRepository, never()).flush();
  }

  private void stubCourseContext() {
    given(courseRepository.findAcademicContextByIdAndInstitution_Id(COURSE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(courseAcademicContext));
    given(courseAcademicContext.getAcademicYearId()).willReturn(ACADEMIC_YEAR_ID);
    given(courseAcademicContext.getStudyPlanId()).willReturn(STUDY_PLAN_ID);
  }
}
