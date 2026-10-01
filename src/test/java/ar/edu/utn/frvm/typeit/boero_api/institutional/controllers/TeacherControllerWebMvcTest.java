package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.InstitutionAccessAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.PermissionAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.RoleAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListInstitutionTeachersUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

@WebMvcTest(TeacherController.class)
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
class TeacherControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_INSTITUTION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private ListInstitutionTeachersUseCase listInstitutionTeachersUseCase;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listTeachers_returnsTeachersForAuthorizedInstitutionalUser() throws Exception {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), "");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(any(), eq(PermissionCode.INSTITUTION_PERSON_READ_ANY)))
        .thenReturn(true);
    when(listInstitutionTeachersUseCase.execute(eq(INSTITUTION_ID), eq("ana"), any(Pageable.class)))
        .thenReturn(
            PaginatedResponse.from(
                new PageImpl<>(
                    List.of(
                        new PersonSummaryResponse(
                            UUID.randomUUID(),
                            "Ana",
                            "García",
                            "12345678",
                            "ana@example.com",
                            null,
                            true,
                            List.of())),
                    Pageable.ofSize(20),
                    1)));

    mockMvc
        .perform(
            get("/api/v1/institutions/{institutionId}/teachers", INSTITUTION_ID)
                .param("search", "ana")
                .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].documentNumber").value("12345678"))
        .andExpect(jsonPath("$.items[0].enabled").value(true));
  }

  @Test
  void listTeachers_returnsForbiddenWithoutPermission() throws Exception {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), "");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(any(), eq(PermissionCode.INSTITUTION_PERSON_READ_ANY)))
        .thenReturn(false);

    mockMvc
        .perform(
            get("/api/v1/institutions/{institutionId}/teachers", INSTITUTION_ID)
                .principal(authentication))
        .andExpect(status().isForbidden());
  }

  @Test
  void listTeachers_returnsForbiddenForAnotherInstitution() throws Exception {
    var authentication =
        new TestingAuthenticationToken(institutionalPrincipal(USER_ID, INSTITUTION_ID), "");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.hasPermission(any(), eq(PermissionCode.INSTITUTION_PERSON_READ_ANY)))
        .thenReturn(true);

    mockMvc
        .perform(
            get("/api/v1/institutions/{institutionId}/teachers", OTHER_INSTITUTION_ID)
                .principal(authentication))
        .andExpect(status().isForbidden());
  }
}
