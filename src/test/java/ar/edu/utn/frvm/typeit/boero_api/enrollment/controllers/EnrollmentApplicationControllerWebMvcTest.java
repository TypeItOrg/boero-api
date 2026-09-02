package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.PermissionAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.CreateEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentStudyPlanSpaceInstrumentOptionsResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentApplicationDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.CreateEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.GetEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationStudyPlanSpacesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationTrainingPathsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListEnrollmentApplicationsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.UpdateEnrollmentApplicationDraftUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@WebMvcTest(EnrollmentApplicationController.class)
@Import({PermissionAuthorizationAspect.class, GlobalExceptionHandler.class, WebConfig.class})
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentApplicationControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
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

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private InstitutionalCallerGuard institutionalCallerGuard;
  @MockitoBean private CreateEnrollmentApplicationUseCase createEnrollmentApplicationUseCase;
  @MockitoBean private ListEnrollmentApplicationsUseCase listEnrollmentApplicationsUseCase;
  @MockitoBean private GetEnrollmentApplicationUseCase getEnrollmentApplicationUseCase;

  @MockitoBean
  private UpdateEnrollmentApplicationDraftUseCase updateEnrollmentApplicationDraftUseCase;

  @MockitoBean
  private ListEnrollmentApplicationTrainingPathsUseCase
      listEnrollmentApplicationTrainingPathsUseCase;

  @MockitoBean
  private ListEnrollmentApplicationStudyPlanSpacesUseCase
      listEnrollmentApplicationStudyPlanSpacesUseCase;

  @MockitoBean
  private ListEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase
      listEnrollmentApplicationStudyPlanSpaceInstrumentsUseCase;

  @Test
  @DisplayName("Should create an enrollment application")
  void createsApplication() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
    when(createEnrollmentApplicationUseCase.execute(
            any(), eq(new CreateEnrollmentApplicationRequest(STUDY_PLAN_ID, ACADEMIC_YEAR_ID))))
        .thenReturn(response(data()));

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications")
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "studyPlanId": "%s",
                      "academicYearId": "%s"
                    }
                    """
                        .formatted(STUDY_PLAN_ID, ACADEMIC_YEAR_ID)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.isEditable").value(true));
  }

  @Test
  @DisplayName("Should list the applicant enrollment applications")
  void listsApplications() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
    when(listEnrollmentApplicationsUseCase.execute(any())).thenReturn(List.of(response(data())));

    mockMvc
        .perform(get("/api/v1/enrollment-applications").principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$[0].studyPlanId").value(STUDY_PLAN_ID.toString()));
  }

  @Test
  @DisplayName("Should replace the draft data including the selected training path")
  void updatesDraft() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
    final ObjectNode data = data();
    when(updateEnrollmentApplicationDraftUseCase.execute(
            any(), eq(APPLICATION_ID), any(UpdateEnrollmentApplicationDraftRequest.class)))
        .thenReturn(response(data));

    mockMvc
        .perform(
            patch("/api/v1/enrollment-applications/{applicationId}/draft", APPLICATION_ID)
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "data": {
                        "careerSelection": {
                          "trainingPathId": "%s"
                        }
                      }
                    }
                    """
                        .formatted(TRAINING_PATH_ID)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.careerSelection.trainingPathId").value(TRAINING_PATH_ID.toString()));
  }

  @Test
  @DisplayName("Should list available training paths for the applicant application")
  void listsTrainingPaths() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
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
                .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(TRAINING_PATH_ID.toString()))
        .andExpect(jsonPath("$[0].name").value("Trayecto A"));
  }

  @Test
  @DisplayName("Should list available study plan spaces for the applicant application")
  void listsStudyPlanSpaces() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
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
                .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(STUDY_PLAN_SPACE_ID.toString()))
        .andExpect(jsonPath("$[0].academicSpaceId").value(ACADEMIC_SPACE_ID.toString()))
        .andExpect(jsonPath("$[0].academicSpaceName").value("Armonia I"));
  }

  @Test
  @DisplayName("Should list available instruments for an applicant study plan space")
  void listsStudyPlanSpaceInstruments() throws Exception {
    final var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
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
                .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studyPlanSpaceId").value(STUDY_PLAN_SPACE_ID.toString()))
        .andExpect(jsonPath("$.requiresInstrument").value(true))
        .andExpect(jsonPath("$.instruments[0].instrumentId").value(INSTRUMENT_ID.toString()))
        .andExpect(jsonPath("$.instruments[0].name").value("Piano"));
  }

  private static EnrollmentApplicationResponse response(final ObjectNode data) {
    return new EnrollmentApplicationResponse(
        APPLICATION_ID,
        UUID.randomUUID(),
        INSTITUTION_ID,
        STUDY_PLAN_ID,
        ACADEMIC_YEAR_ID,
        null,
        EnrollmentApplicationStatus.DRAFT,
        true,
        data,
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  private static ObjectNode data() {
    final ObjectNode data = new ObjectMapper().createObjectNode();
    data.putObject("careerSelection").put("trainingPathId", TRAINING_PATH_ID.toString());
    return data;
  }
}
