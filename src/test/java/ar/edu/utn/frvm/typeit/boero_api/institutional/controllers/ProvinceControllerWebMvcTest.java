package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.ProvinceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CityListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.ProvinceListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListCitiesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListProvincesUseCase;
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

@WebMvcTest(ProvinceController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class ProvinceControllerWebMvcTest {

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
  @MockitoBean private ListProvincesUseCase listProvincesUseCase;
  @MockitoBean private ListCitiesUseCase listCitiesUseCase;

  @Test
  @DisplayName("Should list provinces without authentication")
  void list_returnsProvinces() throws Exception {
    when(listProvincesUseCase.execute(eq(null), any()))
        .thenReturn(
            PaginatedResponse.<ProvinceListItemResponse>builder()
                .items(
                    List.of(
                        ProvinceListItemResponse.builder().id(PROVINCE_ID).name("Cordoba").build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/provinces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(PROVINCE_ID.toString()))
        .andExpect(jsonPath("$.items[0].name").value("Cordoba"));
  }

  @Test
  @DisplayName("Should search provinces by name")
  void list_withSearch() throws Exception {
    when(listProvincesUseCase.execute(eq("cordoba"), any()))
        .thenReturn(
            PaginatedResponse.<ProvinceListItemResponse>builder()
                .items(
                    List.of(
                        ProvinceListItemResponse.builder().id(PROVINCE_ID).name("Córdoba").build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/provinces").param("search", "cordoba"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Córdoba"));
  }

  @Test
  @DisplayName("Should list cities by province without authentication")
  void listCitiesByProvince_returnsCities() throws Exception {
    when(listCitiesUseCase.executeByProvince(eq(PROVINCE_ID), eq(null), any()))
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
        .perform(get("/api/v1/provinces/{provinceId}/cities", PROVINCE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(CITY_ID.toString()))
        .andExpect(jsonPath("$.items[0].name").value("Villa Maria"))
        .andExpect(jsonPath("$.items[0].provinceId").value(PROVINCE_ID.toString()))
        .andExpect(jsonPath("$.items[0].province").value("Cordoba"));
  }

  @Test
  @DisplayName("Should filter cities by province with search")
  void listCitiesByProvince_withSearch() throws Exception {
    when(listCitiesUseCase.executeByProvince(eq(PROVINCE_ID), eq("maria"), any()))
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
        .perform(get("/api/v1/provinces/{provinceId}/cities", PROVINCE_ID).param("search", "maria"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Villa Maria"));
  }

  @Test
  @DisplayName("Should return 404 when province does not exist")
  void listCitiesByProvince_returnsNotFoundForUnknownProvince() throws Exception {
    UUID unknownId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    when(listCitiesUseCase.executeByProvince(eq(unknownId), eq(null), any()))
        .thenThrow(new ProvinceNotFoundException());

    mockMvc
        .perform(get("/api/v1/provinces/{provinceId}/cities", unknownId))
        .andExpect(status().isNotFound());
  }
}
