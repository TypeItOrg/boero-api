package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.MissingRejectionReasonException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.RejectEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RejectEnrollmentApplicationUseCaseTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID APPLICATION_ID = UUID.randomUUID();

  @Mock private EnrollmentApplicationRepository enrollmentApplicationRepository;
  @Mock private EnrollmentApplication application;
  @Mock private StudyPlan studyPlan;
  @Mock private AcademicYear academicYear;
  @Mock private EnrollmentPeriod enrollmentPeriod;

  @Test
  void reject_storesRejectionReasonOnPendingApplication() {
    stubApplication();

    assertThatCode(
            () ->
                useCase()
                    .execute(
                        INSTITUTION_ID,
                        APPLICATION_ID,
                        new RejectEnrollmentApplicationRequest("Documentación incompleta")))
        .doesNotThrowAnyException();

    verify(application).reject(eq("Documentación incompleta"), any());
  }

  @Test
  void reject_propagatesMissingReason() {
    stubApplication();
    willThrow(new MissingRejectionReasonException()).given(application).reject(any(), any());

    assertThatThrownBy(
            () ->
                useCase()
                    .execute(
                        INSTITUTION_ID,
                        APPLICATION_ID,
                        new RejectEnrollmentApplicationRequest(" ")))
        .isInstanceOf(MissingRejectionReasonException.class);
  }

  @Test
  void reject_propagatesStateConflictWhenApplicationAlreadyResolved() {
    stubApplication();
    willThrow(new InvalidEnrollmentApplicationStateException("already resolved"))
        .given(application)
        .reject(any(), any());

    assertThatThrownBy(
            () ->
                useCase()
                    .execute(
                        INSTITUTION_ID,
                        APPLICATION_ID,
                        new RejectEnrollmentApplicationRequest("Documentación incompleta")))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void reject_returnsNotFoundWhenApplicationDoesNotExist() {
    given(
            enrollmentApplicationRepository.findByIdAndInstitutionIdForUpdate(
                INSTITUTION_ID, APPLICATION_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase()
                    .execute(
                        INSTITUTION_ID,
                        APPLICATION_ID,
                        new RejectEnrollmentApplicationRequest("Documentación incompleta")))
        .isInstanceOf(EnrollmentApplicationNotFoundException.class);
  }

  private RejectEnrollmentApplicationUseCase useCase() {
    return new RejectEnrollmentApplicationUseCase(enrollmentApplicationRepository);
  }

  private void stubApplication() {
    final var institution = Institution.builder().id(INSTITUTION_ID).name("Conservatorio").build();
    final var person =
        Person.builder()
            .id(UUID.randomUUID())
            .firstName("Ana")
            .lastName("Garcia")
            .documentNumber("12345678")
            .build();
    given(
            enrollmentApplicationRepository.findByIdAndInstitutionIdForUpdate(
                INSTITUTION_ID, APPLICATION_ID))
        .willReturn(Optional.of(application));
    lenient().when(application.getId()).thenReturn(APPLICATION_ID);
    lenient().when(application.getInstitution()).thenReturn(institution);
    lenient().when(application.getApplicantPerson()).thenReturn(person);
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
