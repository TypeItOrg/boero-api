package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Course;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceFormat;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateCourseRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateCourseUseCaseTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID PLAN_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID SPACE_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID YEAR_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @Mock private InstitutionRepository institutionRepository;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private AcademicSpaceRepository academicSpaceRepository;
  @Mock private AcademicYearRepository academicYearRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseClassAssembler courseClassAssembler;
  @InjectMocks private CreateCourseUseCase useCase;

  private final Institution institution = Institution.builder().id(INSTITUTION_ID).build();

  private void stubInstitution() {
    given(institutionRepository.findById(INSTITUTION_ID)).willReturn(Optional.of(institution));
  }

  private void stubPlan(final StudyPlanStatus status) {
    final var plan =
        org.mockito.Mockito.mock(
            ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan.class);
    given(plan.getId()).willReturn(PLAN_ID);
    given(plan.getName()).willReturn("Plan");
    given(plan.getStatus()).willReturn(status);
    final var path =
        org.mockito.Mockito.mock(
            ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath.class);
    given(path.getId()).willReturn(UUID.randomUUID());
    given(path.getName()).willReturn("Trayecto");
    given(plan.getTrainingPath()).willReturn(path);
    given(studyPlanRepository.findByIdAndInstitution_IdForUpdate(PLAN_ID, INSTITUTION_ID))
        .willReturn(Optional.of(plan));
  }

  private void stubSpace() {
    final var space = org.mockito.Mockito.mock(AcademicSpace.class);
    given(space.getId()).willReturn(SPACE_ID);
    given(space.getName()).willReturn("Espacio");
    given(space.getType()).willReturn(AcademicSpaceType.SUBJECT);
    given(space.getFormat()).willReturn(AcademicSpaceFormat.INDIVIDUAL);
    given(academicSpaceRepository.findByIdAndInstitution_Id(SPACE_ID, INSTITUTION_ID))
        .willReturn(Optional.of(space));
  }

  private void stubYear() {
    final var year = org.mockito.Mockito.mock(AcademicYear.class);
    given(year.getId()).willReturn(YEAR_ID);
    given(year.getYear()).willReturn(2027);
    given(year.getStatus())
        .willReturn(ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus.ACTIVE);
    given(academicYearRepository.findByIdAndInstitution_Id(YEAR_ID, INSTITUTION_ID))
        .willReturn(Optional.of(year));
  }

  private void stubMembership(final boolean member) {
    given(studyPlanSpaceRepository.existsByStudyPlan_IdAndAcademicSpace_Id(PLAN_ID, SPACE_ID))
        .willReturn(member);
  }

  private void stubDuplicate(final boolean duplicated) {
    given(courseRepository.existsByInstitutionAndSpaceAndYear(INSTITUTION_ID, SPACE_ID, YEAR_ID))
        .willReturn(duplicated);
  }

  private CreateCourseRequest request() {
    return new CreateCourseRequest(PLAN_ID, SPACE_ID, YEAR_ID, List.of());
  }

  @Test
  @DisplayName("Should create a course for an active plan containing the space")
  void createsCourseForActivePlan() {
    stubInstitution();
    stubPlan(StudyPlanStatus.ACTIVE);
    stubSpace();
    stubYear();
    stubMembership(true);
    stubDuplicate(false);
    given(courseRepository.save(any(Course.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    final var response = useCase.execute(INSTITUTION_ID, request());

    assertThat(response.studyPlanId()).isEqualTo(PLAN_ID);
    assertThat(response.academicSpaceFormat()).isEqualTo("INDIVIDUAL");
    verify(courseClassAssembler).assemble(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should reject creating a course when the plan is not active")
  void rejectsInactivePlan() {
    stubInstitution();
    stubPlan(StudyPlanStatus.DRAFT);

    assertThatThrownBy(() -> useCase.execute(INSTITUTION_ID, request()))
        .isInstanceOf(AcademicConflictException.class);
    verify(courseRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should reject a course whose space does not belong to the plan")
  void rejectsSpaceOutsidePlan() {
    stubInstitution();
    stubPlan(StudyPlanStatus.ACTIVE);
    stubSpace();
    stubMembership(false);

    assertThatThrownBy(() -> useCase.execute(INSTITUTION_ID, request()))
        .isInstanceOf(AcademicConflictException.class);
  }

  @Test
  @DisplayName("Should reject a duplicate course for the same space and year")
  void rejectsDuplicateSpaceAndYear() {
    stubInstitution();
    stubPlan(StudyPlanStatus.ACTIVE);
    stubSpace();
    stubYear();
    stubMembership(true);
    stubDuplicate(true);

    assertThatThrownBy(() -> useCase.execute(INSTITUTION_ID, request()))
        .isInstanceOf(AcademicConflictException.class);
  }

  @Test
  @DisplayName("Should reject when the institution does not exist")
  void rejectsUnknownInstitution() {
    given(institutionRepository.findById(INSTITUTION_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(INSTITUTION_ID, request()))
        .isInstanceOf(InstitutionNotFoundException.class);
  }
}
