package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceInstrumentOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TrainingPathResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.PermissionAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentStudyPlanSpaceInstrumentOptionsResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PersonalDataDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentApplicationService;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationStudyPlanSpacesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationTrainingPathsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EnrollmentApplicationController.class)
@Import({PermissionAuthorizationAspect.class, GlobalExceptionHandler.class, WebConfig.class})
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentApplicationControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PERSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID APPLICATION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDY_PLAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID ACADEMIC_YEAR_ID =
      UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID TRAINING_PATH_ID =
      UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID STUDY_PLAN_SPACE_ID =
      UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID ACADEMIC_SPACE_ID =
      UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID INSTRUMENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @MockitoBean private org.springframework.util.PathMatcher pathMatcher;

  @MockitoBean
  private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint;

  @MockitoBean private EnrollmentApplicationService applicationService;
  @MockitoBean private InstitutionalCallerGuard institutionalCallerGuard;
  @MockitoBean private AuthorizationService authorizationService;

  @MockitoBean
  private ListEnrollmentApplicationTrainingPathsUseCase
      listEnrollmentApplicationTrainingPathsUseCase;

  @MockitoBean
  private ListEnrollmentApplicationStudyPlanSpacesUseCase
      listEnrollmentApplicationStudyPlanSpacesUseCase;

  @MockitoBean
  private ListEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase
      listEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase;

  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;

  private static TestingAuthenticationToken applicantAuthentication() {
    JwtAuthenticatedUser principal =
        JwtAuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .personId(PERSON_ID)
            .documentNumber("35123456")
            .institutionId(INSTITUTION_ID)
            .sessionId(UUID.randomUUID())
            .tokenId("jti")
            .build();
    return new TestingAuthenticationToken(principal, null);
  }

  private EnrollmentApplicationResponse createMockResponse(EnrollmentApplicationStatus status) {
    return EnrollmentApplicationResponse.builder()
        .applicationId(APPLICATION_ID)
        .institutionId(INSTITUTION_ID)
        .personId(PERSON_ID)
        .studyPlanId(STUDY_PLAN_ID)
        .academicYearId(ACADEMIC_YEAR_ID)
        .enrollmentPeriodId(UUID.randomUUID())
        .status(status)
        .isEditable(status == EnrollmentApplicationStatus.DRAFT)
        .data(
            EnrollmentDraftData.builder()
                .personalData(
                    PersonalDataDto.builder()
                        .firstName("Lucía")
                        .lastName("Gómez")
                        .documentNumber("35123456")
                        .email("lucia@example.com")
                        .build())
                .build())
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  @DisplayName("POST /api/v1/enrollment-applications should start or get application")
  void startOrGetApplication_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.DRAFT);
    when(applicationService.startOrGetApplication(eq(INSTITUTION_ID), eq(PERSON_ID), any()))
        .thenReturn(response);

    StartEnrollmentApplicationRequest request =
        new StartEnrollmentApplicationRequest(STUDY_PLAN_ID, ACADEMIC_YEAR_ID);

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications")
                .principal(applicantAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.isEditable").value(true))
        .andExpect(jsonPath("$.data.personalData.firstName").value("Lucía"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/enrollment-applications/{id} should get own application without review permission")
  void getApplication_selfServiceSuccess() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.DRAFT);
    when(authorizationService.hasPermission(any(), eq(PermissionCode.ENROLLMENT_APPLICATION_READ)))
        .thenReturn(false);
    when(applicationService.getApplicationById(isNull(), eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications/{id}", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.isEditable").value(true));
  }

  @Test
  @DisplayName(
      "GET /api/v1/enrollment-applications/{id} should look up by institution when caller can review")
  void getApplication_institutionalReviewerSuccess() throws Exception {
    EnrollmentApplicationResponse response =
        createMockResponse(EnrollmentApplicationStatus.SUBMITTED);
    when(authorizationService.hasPermission(any(), eq(PermissionCode.ENROLLMENT_APPLICATION_READ)))
        .thenReturn(true);
    when(applicationService.getApplicationById(
            eq(INSTITUTION_ID), eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications/{id}", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.isEditable").value(false));
  }

  @Test
  @DisplayName("PATCH /api/v1/enrollment-applications/{id}/draft should update draft")
  void updateDraft_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.DRAFT);
    when(applicationService.updateDraft(eq(PERSON_ID), eq(APPLICATION_ID), any()))
        .thenReturn(response);

    UpdateEnrollmentDraftRequest request =
        UpdateEnrollmentDraftRequest.builder()
            .data(
                EnrollmentDraftData.builder()
                    .personalData(PersonalDataDto.builder().firstName("Lucía Modificada").build())
                    .build())
            .build();

    mockMvc
        .perform(
            patch("/api/v1/enrollment-applications/{id}/draft", APPLICATION_ID)
                .principal(applicantAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.isEditable").value(true));
  }

  @Test
  @DisplayName("POST /api/v1/enrollment-applications/{id}/cancel should cancel draft")
  void cancelApplication_success() throws Exception {
    EnrollmentApplicationResponse response =
        createMockResponse(EnrollmentApplicationStatus.CANCELLED);
    when(applicationService.cancelApplication(eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications/{id}/cancel", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("POST /api/v1/enrollment-applications/{id}/submit should submit draft")
  void submitApplication_success() throws Exception {
    EnrollmentApplicationResponse response =
        createMockResponse(EnrollmentApplicationStatus.SUBMITTED);
    when(applicationService.submitApplication(eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications/{id}/submit", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test
  @DisplayName("Should list available training paths for the applicant application")
  void listsTrainingPaths() throws Exception {
    when(listEnrollmentApplicationTrainingPathsUseCase.execute(any(), eq(APPLICATION_ID)))
        .thenReturn(
            List.of(
                new TrainingPathResponse(
                    TRAINING_PATH_ID,
                    INSTITUTION_ID,
                    "Conservatorio",
                    "Trayecto A",
                    null,
                    true,
                    null)));

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications/{applicationId}/training-paths", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(TRAINING_PATH_ID.toString()))
        .andExpect(jsonPath("$[0].name").value("Trayecto A"));
  }

  @Test
  @DisplayName("Should list available study plan spaces for the applicant application")
  void listsStudyPlanSpaces() throws Exception {
    when(listEnrollmentApplicationStudyPlanSpacesUseCase.execute(any(), eq(APPLICATION_ID)))
        .thenReturn(
            List.of(
                new StudyPlanSpaceResponse(
                    STUDY_PLAN_SPACE_ID,
                    STUDY_PLAN_ID,
                    ACADEMIC_SPACE_ID,
                    "Armonia I",
                    null,
                    null,
                    RequirementType.REQUIRED,
                    1,
                    ApprovalMode.FINAL_EXAM,
                    false,
                    List.of())));

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications/{applicationId}/study-plan-spaces", APPLICATION_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(STUDY_PLAN_SPACE_ID.toString()))
        .andExpect(jsonPath("$[0].academicSpaceId").value(ACADEMIC_SPACE_ID.toString()))
        .andExpect(jsonPath("$[0].academicSpaceName").value("Armonia I"));
  }

  @Test
  @DisplayName("Should list available instruments for an applicant study plan space")
  void listsStudyPlanSpaceInstruments() throws Exception {
    when(listEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase.execute(
            any(), eq(APPLICATION_ID), eq(STUDY_PLAN_SPACE_ID)))
        .thenReturn(
            new EnrollmentStudyPlanSpaceInstrumentOptionsResponse(
                STUDY_PLAN_SPACE_ID,
                true,
                List.of(new StudyPlanSpaceInstrumentOptionResponse(INSTRUMENT_ID, "Piano"))));

    mockMvc
        .perform(
            get(
                    "/api/v1/enrollment-applications/{applicationId}/study-plan-spaces/{studyPlanSpaceId}/instruments",
                    APPLICATION_ID,
                    STUDY_PLAN_SPACE_ID)
                .principal(applicantAuthentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studyPlanSpaceId").value(STUDY_PLAN_SPACE_ID.toString()))
        .andExpect(jsonPath("$.requiresInstrument").value(true))
        .andExpect(jsonPath("$.instruments[0].instrumentId").value(INSTRUMENT_ID.toString()))
        .andExpect(jsonPath("$.instruments[0].name").value("Piano"));
  }
}
