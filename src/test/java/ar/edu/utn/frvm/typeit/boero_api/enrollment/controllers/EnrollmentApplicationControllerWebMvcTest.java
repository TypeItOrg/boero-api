package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.PersonalDataDto;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(EnrollmentApplicationController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentApplicationControllerWebMvcTest {

  private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PERSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID APPLICATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDY_PLAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @MockitoBean private EnrollmentApplicationService applicationService;
  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;

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
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
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
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .header("X-Person-Id", PERSON_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.data.personalData.firstName").value("Lucía"));
  }

  @Test
  @DisplayName("GET /api/v1/enrollment-applications/{id} should get application")
  void getApplication_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.DRAFT);
    when(applicationService.getApplicationById(eq(INSTITUTION_ID), eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications/{id}", APPLICATION_ID)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .header("X-Person-Id", PERSON_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.status").value("DRAFT"));
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
                .header("X-Person-Id", PERSON_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()));
  }

  @Test
  @DisplayName("POST /api/v1/enrollment-applications/{id}/cancel should cancel draft")
  void cancelApplication_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.CANCELLED);
    when(applicationService.cancelApplication(eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications/{id}/cancel", APPLICATION_ID)
                .header("X-Person-Id", PERSON_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("POST /api/v1/enrollment-applications/{id}/submit should submit draft")
  void submitApplication_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.SUBMITTED);
    when(applicationService.submitApplication(eq(PERSON_ID), eq(APPLICATION_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollment-applications/{id}/submit", APPLICATION_ID)
                .header("X-Person-Id", PERSON_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test
  @DisplayName("GET /api/v1/enrollment-applications should return paginated list")
  void listApplications_success() throws Exception {
    EnrollmentApplicationResponse response = createMockResponse(EnrollmentApplicationStatus.SUBMITTED);
    PaginatedResponse<EnrollmentApplicationResponse> page =
        PaginatedResponse.<EnrollmentApplicationResponse>builder()
            .items(List.of(response))
            .page(0)
            .size(10)
            .totalItems(1)
            .totalPages(1)
            .build();

    when(applicationService.listApplications(eq(INSTITUTION_ID), any(), any(), any(), any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/api/v1/enrollment-applications")
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].applicationId").value(APPLICATION_ID.toString()))
        .andExpect(jsonPath("$.totalItems").value(1));
  }
}
