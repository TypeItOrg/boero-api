package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
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

  @Mock private CourseRepository courseRepository;
  @Mock private AcademicYearRepository academicYearRepository;
  @Mock private AcademicYear academicYear;
  @Mock private Course course;

  @Test
  void rejectsActivatingCourseWhenItsAcademicYearIsClosed() {
    given(courseRepository.findAcademicYearIdByIdAndInstitution_Id(COURSE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(ACADEMIC_YEAR_ID));
    given(
            academicYearRepository.findByIdAndInstitution_IdForUpdate(
                ACADEMIC_YEAR_ID, INSTITUTION_ID))
        .willReturn(Optional.of(academicYear));
    given(academicYear.getStatus()).willReturn(AcademicYearStatus.CLOSED);
    given(courseRepository.findByIdAndInstitution_IdForUpdate(COURSE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(course));

    final var useCase = new UpdateCourseStatusUseCase(courseRepository, academicYearRepository);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    INSTITUTION_ID, COURSE_ID, new CourseStatusRequest(CourseStatus.ACTIVE)))
        .isInstanceOf(AcademicConflictException.class);

    verify(course, never()).updateStatus(any());
    verify(courseRepository, never()).flush();
  }
}
