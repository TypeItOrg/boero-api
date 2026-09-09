package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanStatusRequest;
import ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateStudyPlanStatusUseCaseTest {
  @Spy
  private BusinessDateProvider businessDateProvider = new BusinessDateProvider(Clock.systemUTC());

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PLAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private InstitutionRepository institutionRepository;
  @InjectMocks private UpdateStudyPlanStatusUseCase useCase;

  private StudyPlan activePlan() {
    final var plan =
        StudyPlan.create(
            ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution.builder()
                .id(INSTITUTION_ID)
                .build(),
            ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath.create(
                ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution.builder()
                    .id(INSTITUTION_ID)
                    .build(),
                "Trayecto",
                null),
            "Plan",
            LocalDate.now(ARGENTINA_TIME_ZONE).minusMonths(1),
            null);
    plan.activate();
    return plan;
  }

  @Test
  @DisplayName("Should reject deactivating a plan that still has active courses")
  void rejectsDeactivationWithActiveCourses() {
    final var plan = activePlan();
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(plan));
    given(
            courseRepository
                .existsByInstitution_IdAndStudyPlan_IdAndStatusNotClosedAndDeletedAtIsNull(
                    INSTITUTION_ID, PLAN_ID))
        .willReturn(true);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    INSTITUTION_ID,
                    PLAN_ID,
                    new StudyPlanStatusRequest(
                        StudyPlanStatus.INACTIVE, LocalDate.now(ARGENTINA_TIME_ZONE).minusDays(1))))
        .isInstanceOf(AcademicConflictException.class);
  }

  @Test
  @DisplayName("Should reject deactivating a plan that still has inactive courses")
  void rejectsDeactivationWithInactiveCourses() {
    final var plan = activePlan();
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(plan));
    given(
            courseRepository
                .existsByInstitution_IdAndStudyPlan_IdAndStatusNotClosedAndDeletedAtIsNull(
                    INSTITUTION_ID, PLAN_ID))
        .willReturn(true);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    INSTITUTION_ID,
                    PLAN_ID,
                    new StudyPlanStatusRequest(
                        StudyPlanStatus.INACTIVE, LocalDate.now(ARGENTINA_TIME_ZONE).minusDays(1))))
        .isInstanceOf(AcademicConflictException.class);
  }

  @Test
  @DisplayName("Should deactivate a plan once its courses are closed or deleted")
  void deactivatesWithoutActiveCourses() {
    final var plan = activePlan();
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(plan));
    given(
            courseRepository
                .existsByInstitution_IdAndStudyPlan_IdAndStatusNotClosedAndDeletedAtIsNull(
                    INSTITUTION_ID, PLAN_ID))
        .willReturn(false);

    useCase.execute(
        INSTITUTION_ID,
        PLAN_ID,
        new StudyPlanStatusRequest(
            StudyPlanStatus.INACTIVE, LocalDate.now(ARGENTINA_TIME_ZONE).minusDays(1)));

    verify(studyPlanRepository).flush();
  }
}
