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
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateStudyPlanStatusUseCaseTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PLAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

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
            LocalDate.of(2027, 3, 1),
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
                        StudyPlanStatus.INACTIVE, LocalDate.of(2027, 12, 15))))
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
                        StudyPlanStatus.INACTIVE, LocalDate.of(2027, 12, 15))))
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
        new StudyPlanStatusRequest(StudyPlanStatus.INACTIVE, LocalDate.of(2027, 12, 15)));

    verify(studyPlanRepository).flush();
  }
}
