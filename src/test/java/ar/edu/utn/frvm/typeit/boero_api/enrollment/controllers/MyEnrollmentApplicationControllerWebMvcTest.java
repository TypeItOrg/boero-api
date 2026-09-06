package ar.edu.utn.frvm.typeit.boero_api.enrollment.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.InstitutionAccessAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.PermissionAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.RoleAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InitialRoleAssignmentGuard;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.StartEnrollmentApplicationRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.UpdateEnrollmentDraftRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.GetMyEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.ListMyEnrollmentApplicationsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.StartEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.SubmitEnrollmentApplicationUseCase;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.UpdateEnrollmentDraftUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(MyEnrollmentApplicationController.class)
@Import({
  RoleAuthorizationAspect.class,
  PermissionAuthorizationAspect.class,
  InstitutionAccessAspect.class,
  InstitutionalCallerGuard.class,
  GlobalExceptionHandler.class,
  WebConfig.class
})
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class MyEnrollmentApplicationControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_INSTITUTION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID PERSON_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID APPLICATION_ID =
      UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private InitialRoleAssignmentGuard initialRoleAssignmentGuard;
  @MockitoBean private StartEnrollmentApplicationUseCase startEnrollmentApplicationUseCase;
  @MockitoBean private UpdateEnrollmentDraftUseCase updateEnrollmentDraftUseCase;
  @MockitoBean private SubmitEnrollmentApplicationUseCase submitEnrollmentApplicationUseCase;
  @MockitoBean private ListMyEnrollmentApplicationsUseCase listMyEnrollmentApplicationsUseCase;
  @MockitoBean private GetMyEnrollmentApplicationUseCase getMyEnrollmentApplicationUseCase;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should start an enrollment application for the authenticated applicant")
  void start_createsApplicationOwnedByCurrentPerson() throws Exception {
    when(startEnrollmentApplicationUseCase.execute(
            eq(INSTITUTION_ID), eq(PERSON_ID), any(StartEnrollmentApplicationRequest.class)))
        .thenReturn(response(EnrollmentApplicationStatus.DRAFT));

    mockMvc
        .perform(
            post("/api/v1/institutions/{institutionId}/my-enrollment-applications", INSTITUTION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"studyPlanId\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"academicYearId\":\"22222222-2222-2222-2222-222222222222\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()));

    verify(startEnrollmentApplicationUseCase)
        .execute(eq(INSTITUTION_ID), eq(PERSON_ID), any(StartEnrollmentApplicationRequest.class));
  }

  @Test
  @DisplayName("Should list the authenticated applicant own applications")
  void list_returnsOkScopedToCurrentPerson() throws Exception {
    when(listMyEnrollmentApplicationsUseCase.execute(
            eq(INSTITUTION_ID), eq(PERSON_ID), isNull(), any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(response(EnrollmentApplicationStatus.SUBMITTED)), Pageable.ofSize(20), 1));

    mockMvc
        .perform(
            get("/api/v1/institutions/{institutionId}/my-enrollment-applications", INSTITUTION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].status").value("SUBMITTED"));

    verify(listMyEnrollmentApplicationsUseCase)
        .execute(eq(INSTITUTION_ID), eq(PERSON_ID), isNull(), any(Pageable.class));
  }

  @Test
  @DisplayName("Should get an application owned by the authenticated applicant")
  void get_returnsOkScopedToCurrentPerson() throws Exception {
    when(getMyEnrollmentApplicationUseCase.execute(INSTITUTION_ID, PERSON_ID, APPLICATION_ID))
        .thenReturn(response(EnrollmentApplicationStatus.REJECTED, "Documentación incompleta"));

    mockMvc
        .perform(
            get(
                    "/api/v1/institutions/{institutionId}/my-enrollment-applications/{applicationId}",
                    INSTITUTION_ID,
                    APPLICATION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectionReason").value("Documentación incompleta"));

    verify(getMyEnrollmentApplicationUseCase).execute(INSTITUTION_ID, PERSON_ID, APPLICATION_ID);
  }

  @Test
  @DisplayName("Should update the draft owned by the authenticated applicant")
  void updateDraft_updatesCurrentPersonApplication() throws Exception {
    when(updateEnrollmentDraftUseCase.execute(
            eq(INSTITUTION_ID),
            eq(PERSON_ID),
            eq(APPLICATION_ID),
            any(UpdateEnrollmentDraftRequest.class)))
        .thenReturn(response(EnrollmentApplicationStatus.DRAFT, "Escuela N° 1"));

    mockMvc
        .perform(
            patch(
                    "/api/v1/institutions/{institutionId}/my-enrollment-applications/{applicationId}",
                    INSTITUTION_ID,
                    APPLICATION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null))
                .contentType(APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Garcia\"}"))
        .andExpect(status().isOk());

    verify(updateEnrollmentDraftUseCase)
        .execute(
            eq(INSTITUTION_ID),
            eq(PERSON_ID),
            eq(APPLICATION_ID),
            any(UpdateEnrollmentDraftRequest.class));
  }

  @Test
  @DisplayName("Should submit the draft owned by the authenticated applicant")
  void submit_submitsCurrentPersonApplication() throws Exception {
    when(submitEnrollmentApplicationUseCase.execute(INSTITUTION_ID, PERSON_ID, APPLICATION_ID))
        .thenReturn(response(EnrollmentApplicationStatus.SUBMITTED));

    mockMvc
        .perform(
            post(
                    "/api/v1/institutions/{institutionId}/my-enrollment-applications/{applicationId}/submit",
                    INSTITUTION_ID,
                    APPLICATION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    verify(submitEnrollmentApplicationUseCase).execute(INSTITUTION_ID, PERSON_ID, APPLICATION_ID);
  }

  @Test
  @DisplayName("Should forbid an applicant operating on another institution")
  void start_returnsForbiddenForCrossTenant() throws Exception {
    mockMvc
        .perform(
            post(
                    "/api/v1/institutions/{institutionId}/my-enrollment-applications",
                    OTHER_INSTITUTION_ID)
                .principal(new TestingAuthenticationToken(authentication(), null))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"studyPlanId\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"academicYearId\":\"22222222-2222-2222-2222-222222222222\"}"))
        .andExpect(status().isForbidden());

    verify(startEnrollmentApplicationUseCase, never()).execute(any(), any(), any());
  }

  private JwtAuthenticatedUser authentication() {
    return JwtAuthenticatedUser.builder()
        .userId(UUID.randomUUID())
        .personId(PERSON_ID)
        .documentNumber("12345678")
        .institutionId(INSTITUTION_ID)
        .sessionId(UUID.randomUUID())
        .tokenId("jti")
        .build();
  }

  private static EnrollmentApplicationResponse response(
      final EnrollmentApplicationStatus status, final String rejectionReason) {
    return EnrollmentApplicationResponse.builder()
        .applicationId(APPLICATION_ID)
        .institutionId(INSTITUTION_ID)
        .personId(PERSON_ID)
        .applicantFirstName("Ana")
        .applicantLastName("Garcia")
        .applicantDocumentNumber("12345678")
        .studyPlanId(UUID.randomUUID())
        .studyPlanName("Plan")
        .academicYearId(UUID.randomUUID())
        .academicYear(2026)
        .enrollmentPeriodId(UUID.randomUUID())
        .status(status)
        .isEditable(status == EnrollmentApplicationStatus.DRAFT)
        .secondarySchool(rejectionReason)
        .rejectionReason(status == EnrollmentApplicationStatus.REJECTED ? rejectionReason : null)
        .resolvedAt(LocalDateTime.of(2026, 9, 6, 10, 0))
        .createdAt(LocalDateTime.of(2026, 9, 5, 9, 0))
        .updatedAt(LocalDateTime.of(2026, 9, 6, 10, 0))
        .build();
  }

  private static EnrollmentApplicationResponse response(final EnrollmentApplicationStatus status) {
    return response(status, null);
  }
}
