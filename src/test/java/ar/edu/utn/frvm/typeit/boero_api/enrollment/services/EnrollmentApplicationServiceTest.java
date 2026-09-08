package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EnrollmentApplicationServiceTest {

  @Mock private EnrollmentApplicationRepository applicationRepository;
  @Mock private EnrollmentPeriodRepository periodRepository;
  @Mock private PersonRepository personRepository;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private AcademicYearRepository academicYearRepository;

  private EnrollmentApplicationService service;

  private final UUID institutionId = UUID.randomUUID();
  private final UUID personId = UUID.randomUUID();
  private final UUID studyPlanId = UUID.randomUUID();
  private final UUID academicYearId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID applicationId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new EnrollmentApplicationService(
            applicationRepository,
            periodRepository,
            personRepository,
            studyPlanRepository,
            academicYearRepository);
  }

  @Test
  @DisplayName("Should return existing application if draft already exists")
  void startOrGetApplication_existingDraft() {
    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    EnrollmentApplication existing =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    existing.setId(applicationId);

    when(applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                personId, studyPlanId, academicYearId, EnrollmentApplicationStatus.DRAFT))
        .thenReturn(Optional.of(existing));

    StartEnrollmentApplicationRequest request =
        new StartEnrollmentApplicationRequest(studyPlanId, academicYearId);

    EnrollmentApplicationResponse response =
        service.startOrGetApplication(institutionId, personId, request);

    assertThat(response.getApplicationId()).isEqualTo(applicationId);
    assertThat(response.isEditable()).isTrue();
    assertThat(response.getData().getPersonalData().getFirstName()).isEqualTo("Juan");
  }

  @Test
  @DisplayName("Should throw EnrollmentPeriodClosedException when no active period exists")
  void startOrGetApplication_closedPeriod() {
    when(applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                personId, studyPlanId, academicYearId, EnrollmentApplicationStatus.DRAFT))
        .thenReturn(Optional.empty());

    when(periodRepository.findActivePeriod(
            eq(institutionId),
            eq(academicYearId),
            eq(EnrollmentPeriodStatus.OPEN),
            any(LocalDateTime.class)))
        .thenReturn(Optional.empty());

    StartEnrollmentApplicationRequest request =
        new StartEnrollmentApplicationRequest(studyPlanId, academicYearId);

    assertThatThrownBy(() -> service.startOrGetApplication(institutionId, personId, request))
        .isInstanceOf(EnrollmentPeriodClosedException.class);
  }

  @Test
  @DisplayName("Should create new enrollment application draft when period is open")
  void startOrGetApplication_createNewDraft() {
    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);
    when(period.getInstitution()).thenReturn(institution);

    when(applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                personId, studyPlanId, academicYearId, EnrollmentApplicationStatus.DRAFT))
        .thenReturn(Optional.empty());

    when(periodRepository.findActivePeriod(
            eq(institutionId),
            eq(academicYearId),
            eq(EnrollmentPeriodStatus.OPEN),
            any(LocalDateTime.class)))
        .thenReturn(Optional.of(period));

    when(personRepository.findById(personId)).thenReturn(Optional.of(person));
    when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));

    EnrollmentApplication newApp =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    newApp.setId(applicationId);

    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(newApp);

    StartEnrollmentApplicationRequest request =
        new StartEnrollmentApplicationRequest(studyPlanId, academicYearId);

    EnrollmentApplicationResponse response =
        service.startOrGetApplication(institutionId, personId, request);

    assertThat(response.getApplicationId()).isEqualTo(applicationId);
    assertThat(response.getStatus()).isEqualTo(EnrollmentApplicationStatus.DRAFT);
  }

  @Test
  @DisplayName("Should successfully update draft payload data")
  void updateDraft_success() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");

    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);

    UpdateEnrollmentDraftRequest request =
        new UpdateEnrollmentDraftRequest(
            Map.of(
                "personalData", Map.of("firstName", "Mariano"),
                "academicBackground", Map.of("secondarySchool", "Colegio Nacional")));

    EnrollmentApplicationResponse response = service.updateDraft(personId, applicationId, request);

    verify(person).setFirstName("Mariano");
    verify(applicationRepository).save(application);
  }

  @Test
  @DisplayName("Should throw ApplicationNotEditableException when application status is not DRAFT")
  void updateDraft_notEditable() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .status(EnrollmentApplicationStatus.SUBMITTED)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    UpdateEnrollmentDraftRequest request =
        new UpdateEnrollmentDraftRequest(Map.of("personalData", Map.of("firstName", "Juan")));

    assertThatThrownBy(() -> service.updateDraft(personId, applicationId, request))
        .isInstanceOf(ApplicationNotEditableException.class);
  }

  @Test
  @DisplayName("Should successfully cancel draft application")
  void cancelApplication_success() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);

    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);

    EnrollmentApplicationResponse response = service.cancelApplication(personId, applicationId);

    assertThat(response.getStatus()).isEqualTo(EnrollmentApplicationStatus.CANCELLED);
    verify(applicationRepository).save(application);
  }

  @Test
  @DisplayName("Should throw ApplicationNotEditableException when cancelling non-draft application")
  void cancelApplication_notDraft() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .status(EnrollmentApplicationStatus.SUBMITTED)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.cancelApplication(personId, applicationId))
        .isInstanceOf(ApplicationNotEditableException.class);
  }

  @Test
  @DisplayName("Should successfully submit draft application when mandatory fields are complete")
  void submitApplication_success() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");
    when(person.getEmail()).thenReturn("juan@example.com");
    when(person.getBirthDate()).thenReturn(LocalDate.of(2000, 1, 1));

    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    ApplicantEducationBackground edu =
        ApplicantEducationBackground.builder().secondarySchool("Colegio San Martín").build();

    ApplicantPreference pref = ApplicantPreference.builder().preferredShift("TARDE").build();

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .educationBackground(edu)
            .preference(pref)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);

    EnrollmentApplicationResponse response = service.submitApplication(personId, applicationId);

    assertThat(response.getStatus()).isEqualTo(EnrollmentApplicationStatus.SUBMITTED);
  }

  @Test
  @DisplayName(
      "Should throw EnrollmentValidationException when mandatory fields are missing upon submission")
  void submitApplication_missingFields() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("");

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.submitApplication(personId, applicationId))
        .isInstanceOf(EnrollmentValidationException.class);
  }

  @Test
  @DisplayName("Should list applications paginated for institutional admin")
  void listApplications_success() {
    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.SUBMITTED)
            .build();
    application.setId(applicationId);

    PageImpl<EnrollmentApplication> page = new PageImpl<>(List.of(application));
    when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PaginatedResponse<EnrollmentApplicationResponse> response =
        service.listApplications(
            institutionId,
            periodId,
            EnrollmentApplicationStatus.SUBMITTED,
            null,
            PageRequest.of(0, 10));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).getApplicationId()).isEqualTo(applicationId);
  }
}
