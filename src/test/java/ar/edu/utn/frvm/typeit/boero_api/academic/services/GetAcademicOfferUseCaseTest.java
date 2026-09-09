package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAcademicOfferUseCaseTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID STUDY_PLAN_ID = UUID.randomUUID();
  private static final UUID LEVEL_ID = UUID.randomUUID();

  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private AcademicLevelRepository academicLevelRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;
  @Mock private StudyPlan studyPlan;
  @Mock private TrainingPath trainingPath;
  @Mock private AcademicLevel level;
  @Mock private StudyPlanSpace assignedPlanSpace;
  @Mock private StudyPlanSpace unassignedPlanSpace;
  @Mock private AcademicSpace assignedAcademicSpace;
  @Mock private AcademicSpace unassignedAcademicSpace;

  @Test
  @DisplayName("Should organize active academic spaces by level and preserve unassigned spaces")
  void execute_buildsAvailableCurriculum() {
    stubOffer();

    final var response = useCase().execute(INSTITUTION_ID, STUDY_PLAN_ID);

    assertThat(response.offer().trainingPathName()).isEqualTo("CAVI");
    assertThat(response.levels())
        .singleElement()
        .satisfies(
            offerLevel -> {
              assertThat(offerLevel.name()).isEqualTo("Nivel 1");
              assertThat(offerLevel.spaces())
                  .singleElement()
                  .satisfies(space -> assertThat(space.name()).isEqualTo("Lenguaje Musical"));
            });
    assertThat(response.unassignedSpaces())
        .singleElement()
        .satisfies(space -> assertThat(space.name()).isEqualTo("Taller institucional"));
  }

  private GetAcademicOfferUseCase useCase() {
    return new GetAcademicOfferUseCase(
        studyPlanRepository, academicLevelRepository, studyPlanSpaceRepository);
  }

  private void stubOffer() {
    given(studyPlanRepository.findAvailableOfferById(any(), any(), any()))
        .willReturn(Optional.of(studyPlan));
    given(studyPlanSpaceRepository.findActiveByStudyPlanIdWithDetails(STUDY_PLAN_ID))
        .willReturn(List.of(assignedPlanSpace, unassignedPlanSpace));
    given(academicLevelRepository.findByStudyPlan_IdOrderByDisplayOrderAsc(STUDY_PLAN_ID))
        .willReturn(List.of(level));

    given(studyPlan.getId()).willReturn(STUDY_PLAN_ID);
    given(studyPlan.getName()).willReturn("Plan CAVI 2026");
    given(studyPlan.getVersionNumber()).willReturn(1);
    given(studyPlan.getEffectiveFrom()).willReturn(LocalDate.of(2026, 1, 1));
    given(studyPlan.getTrainingPath()).willReturn(trainingPath);
    given(trainingPath.getId()).willReturn(UUID.randomUUID());
    given(trainingPath.getName()).willReturn("CAVI");

    given(level.getId()).willReturn(LEVEL_ID);
    given(level.getName()).willReturn("Nivel 1");
    given(level.getDisplayOrder()).willReturn(1);

    stubPlanSpace(
        assignedPlanSpace, assignedAcademicSpace, UUID.randomUUID(), "Lenguaje Musical", level);
    stubPlanSpace(
        unassignedPlanSpace,
        unassignedAcademicSpace,
        UUID.randomUUID(),
        "Taller institucional",
        null);
  }

  private static void stubPlanSpace(
      final StudyPlanSpace planSpace,
      final AcademicSpace academicSpace,
      final UUID id,
      final String name,
      final AcademicLevel academicLevel) {
    given(planSpace.getId()).willReturn(id);
    given(planSpace.getAcademicSpace()).willReturn(academicSpace);
    given(planSpace.getAcademicLevel()).willReturn(academicLevel);
    given(planSpace.getRequirementType()).willReturn(RequirementType.REQUIRED);
    given(planSpace.getDisplayOrder()).willReturn(1);
    given(planSpace.getApprovalMode()).willReturn(ApprovalMode.PROMOTION);
    given(academicSpace.getId()).willReturn(UUID.randomUUID());
    given(academicSpace.getName()).willReturn(name);
    given(academicSpace.getType()).willReturn(AcademicSpaceType.SUBJECT);
    given(academicSpace.getFormat()).willReturn(AcademicSpaceFormat.GRUPAL);
  }
}
