package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantEducationBackground;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.ApplicantPreference;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplicationSpace;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ActiveEnrollmentApplicationExistsException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicBackgroundDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.AcademicSpaceSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.InstrumentSelectionDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PersonalDataDto;
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
  @Mock private ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository studyPlanSpaceRepository;
  @Mock private ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository instrumentRepository;
  @Mock private EnrollmentDraftDataValidator enrollmentDraftDataValidator;

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
            academicYearRepository,
            studyPlanSpaceRepository,
            instrumentRepository,
            enrollmentDraftDataValidator);
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

    TrainingPath trainingPath = org.mockito.Mockito.mock(TrainingPath.class);
    when(trainingPath.getId()).thenReturn(UUID.randomUUID());

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getId()).thenReturn(studyPlanId);
    when(studyPlan.getTrainingPath()).thenReturn(trainingPath);

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
    when(applicationRepository.findActiveByApplicantPersonIdAndTrainingPathId(
            personId, trainingPath.getId()))
        .thenReturn(List.of());

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
  @DisplayName(
      "Should throw ActiveEnrollmentApplicationExistsException when person already has a live"
          + " application in the same training path")
  void startOrGetApplication_activeApplicationInSameTrainingPath() {
    Person person = org.mockito.Mockito.mock(Person.class);

    TrainingPath trainingPath = org.mockito.Mockito.mock(TrainingPath.class);
    UUID trainingPathId = UUID.randomUUID();
    when(trainingPath.getId()).thenReturn(trainingPathId);

    StudyPlan studyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(studyPlan.getTrainingPath()).thenReturn(trainingPath);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);

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
    when(applicationRepository.findActiveByApplicantPersonIdAndTrainingPathId(
            personId, trainingPathId))
        .thenReturn(List.of(org.mockito.Mockito.mock(EnrollmentApplication.class)));

    StartEnrollmentApplicationRequest request =
        new StartEnrollmentApplicationRequest(studyPlanId, academicYearId);

    assertThatThrownBy(() -> service.startOrGetApplication(institutionId, personId, request))
        .isInstanceOf(ActiveEnrollmentApplicationExistsException.class);
  }

  @Test
  @DisplayName("Should ignore personalData and persist editable applicant sections")
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
    when(enrollmentDraftDataValidator.validate(eq(institutionId), any(), any()))
        .thenReturn(studyPlan);

    UpdateEnrollmentDraftRequest request =
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .personalData(PersonalDataDto.builder().firstName("Mariano").build())
                    .academicBackground(
                        AcademicBackgroundDto.builder().secondarySchool("Colegio Nacional").build())
                    .build())
            .build();

    EnrollmentApplicationResponse response = service.updateDraft(personId, applicationId, request);

    verify(person, never()).setFirstName(any());
    assertThat(application.getEducationBackground().getSecondarySchool())
        .isEqualTo("Colegio Nacional");
    verify(applicationRepository).save(application);
  }

  @Test
  @DisplayName("Should persist selected study plan spaces and their instruments")
  void updateDraft_persistsSelectedSpacesAndInstruments() {
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

    UUID spaceId = UUID.randomUUID();
    UUID instrumentId = UUID.randomUUID();
    StudyPlanSpace space = org.mockito.Mockito.mock(StudyPlanSpace.class);
    when(space.getId()).thenReturn(spaceId);
    Instrument instrument = org.mockito.Mockito.mock(Instrument.class);
    when(instrument.getId()).thenReturn(instrumentId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);
    when(enrollmentDraftDataValidator.validate(eq(institutionId), any(), any()))
        .thenReturn(studyPlan);
    when(studyPlanSpaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

    UpdateEnrollmentDraftRequest request =
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .academicSpaceSelection(new AcademicSpaceSelectionDto(List.of(spaceId)))
                    .instrumentSelection(
                        new InstrumentSelectionDto(Map.of(spaceId, instrumentId)))
                    .build())
            .build();

    service.updateDraft(personId, applicationId, request);

    assertThat(application.getSelectedSpaces()).hasSize(1);
    EnrollmentApplicationSpace persisted = application.getSelectedSpaces().getFirst();
    assertThat(persisted.getStudyPlanSpace().getId()).isEqualTo(spaceId);
    assertThat(persisted.getInstrument().getId()).isEqualTo(instrumentId);
  }

  @Test
  @DisplayName(
      "Should reassign the study plan and discard previously selected spaces when the career changes")
  void updateDraft_careerChangeReassignsStudyPlanAndClearsSpaces() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);

    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    StudyPlan oldStudyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(oldStudyPlan.getId()).thenReturn(studyPlanId);

    UUID newStudyPlanId = UUID.randomUUID();
    StudyPlan newStudyPlan = org.mockito.Mockito.mock(StudyPlan.class);
    when(newStudyPlan.getId()).thenReturn(newStudyPlanId);

    AcademicYear academicYear = org.mockito.Mockito.mock(AcademicYear.class);
    when(academicYear.getId()).thenReturn(academicYearId);

    EnrollmentPeriod period = org.mockito.Mockito.mock(EnrollmentPeriod.class);
    when(period.getId()).thenReturn(periodId);

    StudyPlanSpace oldSpace = org.mockito.Mockito.mock(StudyPlanSpace.class);
    EnrollmentApplicationSpace previouslySelected =
        EnrollmentApplicationSpace.builder().studyPlanSpace(oldSpace).build();

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(oldStudyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);
    application.addSelectedSpace(previouslySelected);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);
    when(enrollmentDraftDataValidator.validate(eq(institutionId), any(), any()))
        .thenReturn(newStudyPlan);

    UpdateEnrollmentDraftRequest request =
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .careerSelection(
                        new ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CareerSelectionDto(
                            UUID.randomUUID()))
                    .build())
            .build();

    service.updateDraft(personId, applicationId, request);

    assertThat(application.getStudyPlan()).isEqualTo(newStudyPlan);
    assertThat(application.getSelectedSpaces()).isEmpty();
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
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .personalData(PersonalDataDto.builder().firstName("Juan").build())
                    .build())
            .build();

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
    application.addSelectedSpace(
        EnrollmentApplicationSpace.builder()
            .studyPlanSpace(org.mockito.Mockito.mock(StudyPlanSpace.class))
            .build());

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);

    EnrollmentApplicationResponse response = service.submitApplication(personId, applicationId);

    assertThat(response.getStatus()).isEqualTo(EnrollmentApplicationStatus.SUBMITTED);
  }

  @Test
  @DisplayName("Should require at least one selected study plan space to submit")
  void submitApplication_requiresSelectedSpaces() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");
    when(person.getEmail()).thenReturn("juan@example.com");

    ApplicantEducationBackground edu =
        ApplicantEducationBackground.builder().secondarySchool("Colegio San Martín").build();
    ApplicantPreference pref = ApplicantPreference.builder().preferredShift("TARDE").build();

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .educationBackground(edu)
            .preference(pref)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.submitApplication(personId, applicationId))
        .isInstanceOf(EnrollmentValidationException.class)
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(EnrollmentValidationException.class))
        .extracting(EnrollmentValidationException::fieldErrors)
        .satisfies(
            fieldErrors ->
                assertThat(fieldErrors).containsKey("academicSpaceSelection.studyPlanSpaceIds"));
  }

  @Test
  @DisplayName(
      "Should throw EnrollmentValidationException reporting each missing field when mandatory fields are missing upon submission")
  void submitApplication_missingFields() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("");
    when(person.getLastName()).thenReturn(null);
    when(person.getDocumentNumber()).thenReturn(null);
    when(person.getEmail()).thenReturn(null);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.submitApplication(personId, applicationId))
        .isInstanceOf(EnrollmentValidationException.class)
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(EnrollmentValidationException.class))
        .extracting(EnrollmentValidationException::fieldErrors)
        .satisfies(
            fieldErrors -> {
              assertThat(fieldErrors).containsKeys("personalData.firstName", "personalData.lastName", "personalData.documentNumber", "personalData.email");
              assertThat(fieldErrors).containsKeys("academicBackground", "preference.preferredShift");
            });
  }

  @Test
  @DisplayName(
      "Should throw EnrollmentValidationException when a minor applicant has no responsible/tutor data")
  void submitApplication_minorWithoutResponsible_reportsError() {
    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getId()).thenReturn(personId);
    when(person.getFirstName()).thenReturn("Juan");
    when(person.getLastName()).thenReturn("Pérez");
    when(person.getDocumentNumber()).thenReturn("12345678");
    when(person.getEmail()).thenReturn("juan@example.com");
    when(person.getBirthDate()).thenReturn(LocalDate.now().minusYears(16));

    ApplicantEducationBackground edu =
        ApplicantEducationBackground.builder().secondarySchool("Colegio San Martín").build();
    ApplicantPreference pref = ApplicantPreference.builder().preferredShift("TARDE").build();

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .applicantPerson(person)
            .educationBackground(edu)
            .preference(pref)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.submitApplication(personId, applicationId))
        .isInstanceOf(EnrollmentValidationException.class)
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(EnrollmentValidationException.class))
        .extracting(EnrollmentValidationException::fieldErrors)
        .satisfies(fieldErrors -> assertThat(fieldErrors).containsKey("responsible"));
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

  @Test
  @DisplayName("Should deny access to an application belonging to a different applicant")
  void getApplicationById_deniesForeignApplicant() {
    UUID otherPersonId = UUID.randomUUID();
    Person owner = org.mockito.Mockito.mock(Person.class);
    when(owner.getId()).thenReturn(personId);

    EnrollmentApplication application =
        EnrollmentApplication.builder().applicantPerson(owner).status(EnrollmentApplicationStatus.SUBMITTED).build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.getApplicationById(otherPersonId, applicationId))
        .isInstanceOf(
            ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException
                .class);
  }

  @Test
  @DisplayName("Should deny institutional access to an application from a different institution")
  void getApplicationById_deniesForeignInstitution() {
    UUID otherInstitutionId = UUID.randomUUID();
    Institution institution = org.mockito.Mockito.mock(Institution.class);
    when(institution.getId()).thenReturn(institutionId);

    Person owner = org.mockito.Mockito.mock(Person.class);

    EnrollmentApplication application =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(owner)
            .status(EnrollmentApplicationStatus.SUBMITTED)
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.getApplicationById(otherInstitutionId, null, applicationId))
        .isInstanceOf(
            ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException
                .class);
  }
}
