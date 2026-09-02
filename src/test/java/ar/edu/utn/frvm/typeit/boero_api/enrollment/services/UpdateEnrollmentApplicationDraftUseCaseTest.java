package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentApplicationDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UpdateEnrollmentApplicationDraftUseCaseTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private PersonRepository personRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private EnrollmentApplicationRepository enrollmentApplicationRepository;
  @Mock private TrainingPathRepository trainingPathRepository;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;

  @Test
  @DisplayName("Should reject draft updates when the application is not editable")
  void rejectsNotEditableApplication() {
    final var application = application();
    application.cancel();
    final var principal = principal(application);
    final var useCase = useCase();
    givenApplicant(principal, application.getPerson());
    given(
            enrollmentApplicationRepository.findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
                application.getId(), principal.personId(), principal.institutionId()))
        .willReturn(Optional.of(application));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    principal,
                    application.getId(),
                    new UpdateEnrollmentApplicationDraftRequest(OBJECT_MAPPER.createObjectNode())))
        .isInstanceOf(EnrollmentApplicationNotEditableException.class);
  }

  @Test
  @DisplayName("Should replace the full draft data after validating the training path")
  void replacesFullDraftData() {
    final var application = application();
    final var principal = principal(application);
    final var useCase = useCase();
    final var trainingPath = mock(TrainingPath.class);
    final var trainingPathId = UUID.randomUUID();
    final var data = OBJECT_MAPPER.createObjectNode();
    data.putObject("careerSelection").put("trainingPathId", trainingPathId.toString());
    final var request = new UpdateEnrollmentApplicationDraftRequest(data);
    givenApplicant(principal, application.getPerson());
    given(
            enrollmentApplicationRepository.findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
                application.getId(), principal.personId(), principal.institutionId()))
        .willReturn(Optional.of(application));
    given(
            trainingPathRepository.findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(
                trainingPathId, principal.institutionId()))
        .willReturn(Optional.of(trainingPath));
    given(
            studyPlanRepository.findActiveByTrainingPathIdAndInstitutionIdValidOn(
                trainingPathId,
                principal.institutionId(),
                StudyPlanStatus.ACTIVE,
                application.getAcademicYear().getStartDate()))
        .willReturn(List.of(application.getStudyPlan()));
    given(enrollmentApplicationRepository.save(application)).willReturn(application);

    useCase.execute(principal, application.getId(), request);

    verify(enrollmentApplicationRepository).save(application);
  }

  @Test
  @DisplayName("Should reject draft updates when selected study plan spaces are invalid")
  void rejectsInvalidSelectedStudyPlanSpaces() {
    final var application = application();
    final var principal = principal(application);
    final var useCase = useCase();
    final var data = OBJECT_MAPPER.createObjectNode();
    data.putObject("academicSpaceSelection").putArray("studyPlanSpaceIds").add("not-a-uuid");
    givenApplicant(principal, application.getPerson());
    given(
            enrollmentApplicationRepository.findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
                application.getId(), principal.personId(), principal.institutionId()))
        .willReturn(Optional.of(application));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    principal,
                    application.getId(),
                    new UpdateEnrollmentApplicationDraftRequest(data)))
        .isInstanceOf(EnrollmentValidationException.class);
  }

  private UpdateEnrollmentApplicationDraftUseCase useCase() {
    return new UpdateEnrollmentApplicationDraftUseCase(
        new ApplicantEnrollmentGuard(personRepository, personRoleAssignmentRepository),
        enrollmentApplicationRepository,
        new EnrollmentDraftDataValidator(
            trainingPathRepository, studyPlanRepository, studyPlanSpaceRepository));
  }

  private void givenApplicant(final JwtAuthenticatedUser principal, final Person person) {
    given(
            personRoleAssignmentRepository.existsByPerson_IdAndInstitution_IdAndRole_Code(
                principal.personId(), principal.institutionId(), "APPLICANT"))
        .willReturn(true);
    given(
            personRepository.findByIdAndInstitution_Id(
                principal.personId(), principal.institutionId()))
        .willReturn(Optional.of(person));
  }

  private static EnrollmentApplication application() {
    final var institution = Institution.builder().id(UUID.randomUUID()).build();
    final var person =
        Person.builder()
            .id(UUID.randomUUID())
            .institution(institution)
            .firstName("Ana")
            .lastName("Garcia")
            .documentNumber("12345678")
            .email("ana@example.com")
            .build();
    final var path = TrainingPath.create(institution, "Base", null);
    final StudyPlan plan =
        StudyPlan.create(institution, path, "Plan", LocalDate.of(2026, 3, 1), null);
    final AcademicYear year =
        AcademicYear.create(institution, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1));
    return EnrollmentApplication.create(person, institution, plan, year, null);
  }

  private static JwtAuthenticatedUser principal(final EnrollmentApplication application) {
    return JwtAuthenticatedUser.builder()
        .userId(UUID.randomUUID())
        .personId(application.getPerson().getId())
        .documentNumber("12345678")
        .institutionId(application.getInstitution().getId())
        .sessionId(UUID.randomUUID())
        .tokenId("token-id")
        .build();
  }
}
