package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseSpaceOptionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCourseSpaceOptionsUseCaseTest {

  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID STUDY_PLAN_ID = UUID.randomUUID();
  private static final UUID ACADEMIC_SPACE_ID = UUID.randomUUID();

  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;
  @Mock private StudyPlan studyPlan;
  @Mock private StudyPlanSpace studyPlanSpace;
  @Mock private AcademicSpace academicSpace;

  @Test
  void searchesAcademicSpacesWithoutConsideringAccents() {
    stubActivePlanAndSpace("Educación Física");

    final var result = useCase().execute(INSTITUTION_ID, STUDY_PLAN_ID, " educacion ");

    assertThat(result)
        .containsExactly(
            new CourseSpaceOptionResponse(
                ACADEMIC_SPACE_ID, "Educación Física", "SUBJECT", "INDIVIDUAL"));
  }

  @Test
  void ignoresBlankSearchValues() {
    stubActivePlanAndSpace("Educación Física");

    final var result = useCase().execute(INSTITUTION_ID, STUDY_PLAN_ID, "  ");

    assertThat(result).hasSize(1);
  }

  private ListCourseSpaceOptionsUseCase useCase() {
    return new ListCourseSpaceOptionsUseCase(studyPlanRepository, studyPlanSpaceRepository);
  }

  private void stubActivePlanAndSpace(final String name) {
    given(studyPlanRepository.findByIdAndInstitution_Id(STUDY_PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(studyPlan));
    given(studyPlan.getStatus()).willReturn(StudyPlanStatus.ACTIVE);
    given(studyPlanSpaceRepository.findByStudyPlanIdWithDetails(STUDY_PLAN_ID))
        .willReturn(List.of(studyPlanSpace));
    given(studyPlanSpace.getAcademicSpace()).willReturn(academicSpace);
    given(academicSpace.getId()).willReturn(ACADEMIC_SPACE_ID);
    given(academicSpace.getName()).willReturn(name);
    given(academicSpace.getType()).willReturn(AcademicSpaceType.SUBJECT);
    given(academicSpace.getFormat()).willReturn(AcademicSpaceFormat.INDIVIDUAL);
  }
}
