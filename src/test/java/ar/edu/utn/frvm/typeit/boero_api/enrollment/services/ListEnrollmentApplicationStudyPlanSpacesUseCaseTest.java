package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
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

@ExtendWith(MockitoExtension.class)
class ListEnrollmentApplicationStudyPlanSpacesUseCaseTest {

  @Mock private PersonRepository personRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private EnrollmentApplicationRepository enrollmentApplicationRepository;
  @Mock private StudyPlanSpaceRepository studyPlanSpaceRepository;

  @Test
  @DisplayName("Should list eligible study plan spaces using the application study plan")
  void listsEligibleStudyPlanSpaces() {
    final var application = application();
    final var principal = principal(application);
    final var academicSpace =
        AcademicSpace.create(
            application.getInstitution(), "Armonia I", "", AcademicSpaceType.SUBJECT);
    final var studyPlanSpace =
        StudyPlanSpace.create(
            application.getInstitution(),
            application.getStudyPlan(),
            academicSpace,
            null,
            RequirementType.REQUIRED,
            1,
            ApprovalMode.FINAL_EXAM);
    final var useCase =
        new ListEnrollmentApplicationStudyPlanSpacesUseCase(
            new ApplicantEnrollmentGuard(personRepository, personRoleAssignmentRepository),
            enrollmentApplicationRepository,
            studyPlanSpaceRepository);
    givenApplicant(principal, application.getPerson());
    given(
            enrollmentApplicationRepository.findByIdAndPerson_IdAndInstitution_IdAndDeletedAtIsNull(
                application.getId(), principal.personId(), principal.institutionId()))
        .willReturn(Optional.of(application));
    given(
            studyPlanSpaceRepository.findEligibleByStudyPlanId(
                principal.institutionId(), application.getStudyPlan().getId()))
        .willReturn(List.of(studyPlanSpace));

    final var response = useCase.execute(principal, application.getId());

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().studyPlanId()).isEqualTo(application.getStudyPlan().getId());
    assertThat(response.getFirst().academicSpaceName()).isEqualTo("Armonia I");
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
    final var plan = StudyPlan.create(institution, path, "Plan", LocalDate.of(2026, 3, 1), null);
    final var year =
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
