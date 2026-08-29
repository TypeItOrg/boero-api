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
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentPeriod;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentPeriodStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentPeriodClosedException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentPeriodRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentApplicationServiceTest {

  @Mock private EnrollmentApplicationRepository applicationRepository;
  @Mock private EnrollmentPeriodRepository periodRepository;
  @Mock private PersonRepository personRepository;
  @Mock private StudyPlanRepository studyPlanRepository;
  @Mock private AcademicYearRepository academicYearRepository;

  private EnrollmentApplicationService service;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
            objectMapper);
  }

  @Test
  @DisplayName("Should return existing application if draft already exists")
  void startOrGetApplication_existingDraft() {
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

    EnrollmentApplication existing =
        EnrollmentApplication.builder()
            .institution(institution)
            .applicantPerson(person)
            .studyPlan(studyPlan)
            .academicYear(academicYear)
            .enrollmentPeriod(period)
            .status(EnrollmentApplicationStatus.DRAFT)
            .data("{\"key\":\"value\"}")
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
    assertThat(response.getData()).containsEntry("key", "value");
  }

  @Test
  @DisplayName("Should throw EnrollmentPeriodClosedException when no active period exists")
  void startOrGetApplication_closedPeriod() {
    when(applicationRepository
            .findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                personId, studyPlanId, academicYearId, EnrollmentApplicationStatus.DRAFT))
        .thenReturn(Optional.empty());

    when(periodRepository.findActivePeriod(
            eq(institutionId), eq(academicYearId), eq(EnrollmentPeriodStatus.OPEN), any(LocalDateTime.class)))
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
            eq(institutionId), eq(academicYearId), eq(EnrollmentPeriodStatus.OPEN), any(LocalDateTime.class)))
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
            .data("{}")
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
            .data("{}")
            .build();
    application.setId(applicationId);

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(applicationRepository.save(any(EnrollmentApplication.class))).thenReturn(application);

    UpdateEnrollmentDraftRequest request =
        new UpdateEnrollmentDraftRequest(Map.of("firstName", "Juan"));

    EnrollmentApplicationResponse response = service.updateDraft(personId, applicationId, request);

    assertThat(response.getData()).containsEntry("firstName", "Juan");
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
        new UpdateEnrollmentDraftRequest(Map.of("firstName", "Juan"));

    assertThatThrownBy(() -> service.updateDraft(personId, applicationId, request))
        .isInstanceOf(ApplicationNotEditableException.class);
  }
}
