package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Student;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.StudentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApproveEnrollmentApplicationUseCaseTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID APPLICATION_ID = UUID.randomUUID();
  private static final Person PERSON =
      Person.builder()
          .id(UUID.randomUUID())
          .firstName("Ana")
          .lastName("Garcia")
          .documentNumber("12345678")
          .build();

  @Mock private EnrollmentApplicationRepository enrollmentApplicationRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private EnrollmentApplication application;
  @Mock private StudyPlan studyPlan;
  @Mock private AcademicYear academicYear;
  @Mock private EnrollmentPeriod enrollmentPeriod;

  @Test
  void approve_createsStudentOnFirstApproval() {
    stubApplication();
    given(application.getApplicantPerson()).willReturn(PERSON);
    given(studentRepository.existsByInstitution_IdAndPerson_Id(INSTITUTION_ID, PERSON.getId()))
        .willReturn(false);
    given(studentRepository.countByInstitution_Id(INSTITUTION_ID)).willReturn(0L);

    assertThatCode(() -> useCase().execute(INSTITUTION_ID, APPLICATION_ID))
        .doesNotThrowAnyException();

    verify(application).approve(any());
    verify(studentRepository).save(any(Student.class));
  }

  @Test
  void approve_doesNotCreateStudentWhenAlreadyEnrolled() {
    stubApplication();
    given(application.getApplicantPerson()).willReturn(PERSON);
    given(studentRepository.existsByInstitution_IdAndPerson_Id(INSTITUTION_ID, PERSON.getId()))
        .willReturn(true);

    useCase().execute(INSTITUTION_ID, APPLICATION_ID);

    verify(studentRepository, never()).save(any(Student.class));
    verify(studentRepository, never()).countByInstitution_Id(INSTITUTION_ID);
  }

  @Test
  void approve_propagatesStateConflictWhenApplicationAlreadyResolved() {
    stubApplication();
    willThrow(new InvalidEnrollmentApplicationStateException("already resolved"))
        .given(application)
        .approve(any());

    assertThatThrownBy(() -> useCase().execute(INSTITUTION_ID, APPLICATION_ID))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);

    verify(studentRepository, never()).countByInstitution_Id(INSTITUTION_ID);
  }

  @Test
  void approve_returnsNotFoundWhenApplicationDoesNotExist() {
    given(
            enrollmentApplicationRepository.findByIdAndInstitutionIdForUpdate(
                INSTITUTION_ID, APPLICATION_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> useCase().execute(INSTITUTION_ID, APPLICATION_ID))
        .isInstanceOf(EnrollmentApplicationNotFoundException.class);

    verify(studentRepository, never()).save(any(Student.class));
  }

  private ApproveEnrollmentApplicationUseCase useCase() {
    return new ApproveEnrollmentApplicationUseCase(
        enrollmentApplicationRepository, studentRepository);
  }

  private void stubApplication() {
    final var institution = Institution.builder().id(INSTITUTION_ID).name("Conservatorio").build();
    given(
            enrollmentApplicationRepository.findByIdAndInstitutionIdForUpdate(
                INSTITUTION_ID, APPLICATION_ID))
        .willReturn(Optional.of(application));
    lenient().when(application.getId()).thenReturn(APPLICATION_ID);
    lenient().when(application.getInstitution()).thenReturn(institution);
    lenient().when(application.getStudyPlan()).thenReturn(studyPlan);
    lenient().when(studyPlan.getId()).thenReturn(UUID.randomUUID());
    lenient().when(studyPlan.getName()).thenReturn("Plan");
    lenient().when(application.getAcademicYear()).thenReturn(academicYear);
    lenient().when(academicYear.getId()).thenReturn(UUID.randomUUID());
    lenient().when(academicYear.getYear()).thenReturn(2026);
    lenient().when(application.getEnrollmentPeriod()).thenReturn(enrollmentPeriod);
    lenient().when(enrollmentPeriod.getId()).thenReturn(UUID.randomUUID());
    lenient().when(application.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 5, 9, 0));
    lenient().when(application.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 9, 5, 9, 0));
  }
}
