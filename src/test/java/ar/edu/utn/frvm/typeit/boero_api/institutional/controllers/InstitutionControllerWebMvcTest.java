package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.platformPrincipal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.RoleAuthorizationAspect;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.CreateInstitutionRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.CreateInstitutionUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.GetInstitutionUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListInstitutionsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.UpdateInstitutionUseCase;
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

@WebMvcTest(InstitutionController.class)
@Import({RoleAuthorizationAspect.class, GlobalExceptionHandler.class, WebConfig.class})
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class InstitutionControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID CITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID PLATFORM_ACCOUNT_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private ListInstitutionsUseCase listInstitutionsUseCase;
  @MockitoBean private GetInstitutionUseCase getInstitutionUseCase;
  @MockitoBean private CreateInstitutionUseCase createInstitutionUseCase;
  @MockitoBean private UpdateInstitutionUseCase updateInstitutionUseCase;

  @Test
  @DisplayName("Should list institutions without authentication")
  void list_returnsPublicInstitutions() throws Exception {
    when(listInstitutionsUseCase.execute(any()))
        .thenReturn(
            PaginatedResponse.<InstitutionListItemResponse>builder()
                .items(
                    List.of(
                        InstitutionListItemResponse.builder()
                            .id(INSTITUTION_ID)
                            .name("Conservatorio Boero")
                            .slug("boero-villa-maria")
                            .city("Villa Maria")
                            .province("Cordoba")
                            .build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/institutions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(INSTITUTION_ID.toString()))
        .andExpect(jsonPath("$.items[0].slug").value("boero-villa-maria"))
        .andExpect(jsonPath("$.items[0].email").doesNotExist())
        .andExpect(jsonPath("$.items[0].phoneNumber").doesNotExist());
  }

  @Test
  @DisplayName("Should return institution detail without authentication")
  void get_returnsPublicInstitutionDetail() throws Exception {
    when(getInstitutionUseCase.execute(INSTITUTION_ID)).thenReturn(detailResponse());

    mockMvc
        .perform(get("/api/v1/institutions/{id}", INSTITUTION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(INSTITUTION_ID.toString()))
        .andExpect(jsonPath("$.slug").value("boero-villa-maria"));
  }

  @Test
  @DisplayName("Should forbid unauthenticated institution creation")
  void create_returnsForbiddenWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/institutions").contentType(APPLICATION_JSON).content(createPayload()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should forbid institutional principals from creating institutions")
  void create_returnsForbiddenForInstitutionalPrincipal() throws Exception {
    var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
    stubPlatformAdminAccess(false);

    mockMvc
        .perform(
            post("/api/v1/institutions")
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(createPayload()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should create institution for platform admin")
  void create_returnsCreatedForPlatformAdmin() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);
    when(createInstitutionUseCase.execute(any(CreateInstitutionRequest.class)))
        .thenReturn(detailResponse());

    mockMvc
        .perform(
            post("/api/v1/institutions")
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(createPayload()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slug").value("boero-villa-maria"));
  }

  @Test
  @DisplayName("Should forbid institutional principals from updating institutions")
  void update_returnsForbiddenForInstitutionalPrincipal() throws Exception {
    var authentication =
        new TestingAuthenticationToken(
            institutionalPrincipal(UUID.randomUUID(), INSTITUTION_ID), null);
    stubPlatformAdminAccess(false);

    mockMvc
        .perform(
            put("/api/v1/institutions/{id}", INSTITUTION_ID)
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(updatePayload()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should update institution for platform admin")
  void update_returnsOkForPlatformAdmin() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);
    when(updateInstitutionUseCase.execute(eq(INSTITUTION_ID), any(UpdateInstitutionRequest.class)))
        .thenReturn(detailResponse());

    mockMvc
        .perform(
            put("/api/v1/institutions/{id}", INSTITUTION_ID)
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(updatePayload()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(INSTITUTION_ID.toString()));
  }

  @Test
  @DisplayName("Should return bad request when city does not exist on create")
  void create_returnsBadRequestWhenCityNotFound() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);
    when(createInstitutionUseCase.execute(any(CreateInstitutionRequest.class)))
        .thenThrow(new CityNotFoundException());

    mockMvc
        .perform(
            post("/api/v1/institutions")
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(createPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La ciudad especificada no existe."));
  }

  @Test
  @DisplayName("Should return bad request when cityId is malformed on create")
  void create_returnsBadRequestWhenCityIdIsInvalid() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);

    mockMvc
        .perform(
            post("/api/v1/institutions")
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(invalidCityIdPayload()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return bad request when city does not exist on update")
  void update_returnsBadRequestWhenCityNotFound() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);
    when(updateInstitutionUseCase.execute(eq(INSTITUTION_ID), any(UpdateInstitutionRequest.class)))
        .thenThrow(new CityNotFoundException());

    mockMvc
        .perform(
            put("/api/v1/institutions/{id}", INSTITUTION_ID)
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(updatePayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La ciudad especificada no existe."));
  }

  @Test
  @DisplayName("Should return bad request when cityId is malformed on update")
  void update_returnsBadRequestWhenCityIdIsInvalid() throws Exception {
    var authentication =
        new TestingAuthenticationToken(platformPrincipal(PLATFORM_ACCOUNT_ID), null);
    stubPlatformAdminAccess(true);

    mockMvc
        .perform(
            put("/api/v1/institutions/{id}", INSTITUTION_ID)
                .principal(authentication)
                .contentType(APPLICATION_JSON)
                .content(invalidCityIdPayload()))
        .andExpect(status().isBadRequest());
  }

  private void stubPlatformAdminAccess(boolean allowed) {
    when(authorizationService.hasPlatformRole(any(), eq(PlatformRoleCode.PLATFORM_ADMIN)))
        .thenReturn(allowed);
  }

  private static InstitutionDetailResponse detailResponse() {
    return InstitutionDetailResponse.builder()
        .id(INSTITUTION_ID)
        .name("Conservatorio Boero")
        .slug("boero-villa-maria")
        .city("Villa Maria")
        .province("Cordoba")
        .street("San Martin")
        .number("123")
        .neighborhood("Centro")
        .phoneNumber("0353-123456")
        .email("info@boero.edu.ar")
        .active(true)
        .build();
  }

  private static String createPayload() {
    return """
        {
          "name": "Conservatorio Boero",
          "slug": "boero-villa-maria",
          "cityId": "%s"
        }
        """
        .formatted(CITY_ID);
  }

  private static String updatePayload() {
    return """
        {
          "name": "Conservatorio Boero",
          "slug": "boero-villa-maria",
          "cityId": "%s",
          "active": true
        }
        """
        .formatted(CITY_ID);
  }

  private static String invalidCityIdPayload() {
    return """
        {
          "name": "Conservatorio Boero",
          "slug": "boero-villa-maria",
          "cityId": "not-a-uuid"
        }
        """;
  }
}
