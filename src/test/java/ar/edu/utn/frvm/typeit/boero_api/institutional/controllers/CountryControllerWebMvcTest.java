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
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CountryNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CountrySummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.ProvinceListItemResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.ListCountriesUseCase;
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

@WebMvcTest(CountryController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class CountryControllerWebMvcTest {

  private static final UUID COUNTRY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PROVINCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;
  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private ListCountriesUseCase listCountriesUseCase;
  @MockitoBean private ListProvincesUseCase listProvincesUseCase;

  @Test
  @DisplayName("Should list countries without authentication")
  void list_returnsCountries() throws Exception {
    when(listCountriesUseCase.execute(eq(null), any()))
        .thenReturn(
            PaginatedResponse.<CountrySummaryResponse>builder()
                .items(
                    List.of(
                        CountrySummaryResponse.builder()
                            .id(COUNTRY_ID)
                            .name("Argentina")
                            .isoCode("ARG")
                            .build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/countries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(COUNTRY_ID.toString()))
        .andExpect(jsonPath("$.items[0].name").value("Argentina"))
        .andExpect(jsonPath("$.items[0].isoCode").value("ARG"));
  }

  @Test
  @DisplayName("Should search countries by name")
  void list_withSearch() throws Exception {
    when(listCountriesUseCase.execute(eq("argentina"), any()))
        .thenReturn(
            PaginatedResponse.<CountrySummaryResponse>builder()
                .items(
                    List.of(
                        CountrySummaryResponse.builder()
                            .id(COUNTRY_ID)
                            .name("Argentina")
                            .isoCode("ARG")
                            .build()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

    mockMvc
        .perform(get("/api/v1/countries").param("search", "argentina"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Argentina"));
  }

  @Test
  @DisplayName("Should list provinces by country without authentication")
  void listProvincesByCountry_returnsProvinces() throws Exception {
    when(listProvincesUseCase.executeByCountry(eq(COUNTRY_ID), eq(null), any()))
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
        .perform(get("/api/v1/countries/{countryId}/provinces", COUNTRY_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(PROVINCE_ID.toString()))
        .andExpect(jsonPath("$.items[0].name").value("Cordoba"));
  }

  @Test
  @DisplayName("Should filter provinces by country with search")
  void listProvincesByCountry_withSearch() throws Exception {
    when(listProvincesUseCase.executeByCountry(eq(COUNTRY_ID), eq("cordoba"), any()))
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
        .perform(
            get("/api/v1/countries/{countryId}/provinces", COUNTRY_ID).param("search", "cordoba"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Córdoba"));
  }

  @Test
  @DisplayName("Should return 404 when country does not exist")
  void listProvincesByCountry_returnsNotFoundForUnknownCountry() throws Exception {
    UUID unknownId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    when(listProvincesUseCase.executeByCountry(eq(unknownId), eq(null), any()))
        .thenThrow(new CountryNotFoundException());

    mockMvc
        .perform(get("/api/v1/countries/{countryId}/provinces", unknownId))
        .andExpect(status().isNotFound());
  }
}
