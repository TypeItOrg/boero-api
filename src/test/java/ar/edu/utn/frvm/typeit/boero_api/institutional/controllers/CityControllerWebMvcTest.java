package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

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
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CityListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListCitiesUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(CityController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class CityControllerWebMvcTest {

  private static final UUID PROVINCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID CITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private ListCitiesUseCase listCitiesUseCase;

  @Test
  @DisplayName("Should list cities without authentication")
  void list_returnsCities() throws Exception {
    when(listCitiesUseCase.execute(eq(null), any()))
        .thenReturn(
            PaginatedResponse.<CityListItemResponse>builder()
                .items(
                    List.of(
                        CityListItemResponse.builder()
                            .id(CITY_ID)
                            .name("Villa Maria")
                            .provinceId(PROVINCE_ID)
                            .province("Cordoba")
                            .build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(CITY_ID.toString()))
        .andExpect(jsonPath("$.items[0].name").value("Villa Maria"))
        .andExpect(jsonPath("$.items[0].provinceId").value(PROVINCE_ID.toString()))
        .andExpect(jsonPath("$.items[0].province").value("Cordoba"));
  }

  @Test
  @DisplayName("Should search cities by name or province")
  void list_withSearch() throws Exception {
    when(listCitiesUseCase.execute(eq("cordoba"), any()))
        .thenReturn(
            PaginatedResponse.<CityListItemResponse>builder()
                .items(
                    List.of(
                        CityListItemResponse.builder()
                            .id(CITY_ID)
                            .name("Villa Maria")
                            .provinceId(PROVINCE_ID)
                            .province("Cordoba")
                            .build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/cities").param("search", "cordoba"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].province").value("Cordoba"));
  }

  @Test
  @DisplayName("Should return empty list when no cities match search")
  void list_returnsEmptyWhenNoMatches() throws Exception {
    when(listCitiesUseCase.execute(eq("xyzabc"), any()))
        .thenReturn(
            PaginatedResponse.<CityListItemResponse>builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalItems(0)
                .totalPages(0)
                .build());

    mockMvc
        .perform(get("/api/v1/cities").param("search", "xyzabc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }
}
