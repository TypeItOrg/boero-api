package ar.edu.utn.frvm.typeit.boero_api.academic.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicOfferDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicOfferSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.services.GetAcademicOfferUseCase;
import ar.edu.utn.frvm.typeit.boero_api.academic.services.ListAcademicOffersUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(AcademicOfferController.class)
@AutoConfigureMockMvc(addFilters = false)
class AcademicOfferControllerWebMvcTest {

  private static final UUID INSTITUTION_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID STUDY_PLAN_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListAcademicOffersUseCase listAcademicOffersUseCase;
  @MockitoBean private GetAcademicOfferUseCase getAcademicOfferUseCase;
  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;
  @MockitoBean private IsPlatformSessionActiveUseCase isPlatformSessionActiveUseCase;

  @Test
  @DisplayName("Should list available academic offers with the default path sort")
  void list_returnsAvailableOffers() throws Exception {
    final var offer = offer();
    when(listAcademicOffersUseCase.execute(eq(INSTITUTION_ID), any(Pageable.class)))
        .thenReturn(new PaginatedResponse<>(List.of(offer), 0, 10, 1, 1));

    mockMvc
        .perform(get("/api/v1/institutions/" + INSTITUTION_ID + "/academic-offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].trainingPathName").value("CAVI"))
        .andExpect(jsonPath("$.items[0].studyPlanId").value(STUDY_PLAN_ID.toString()));

    final var pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(listAcademicOffersUseCase).execute(eq(INSTITUTION_ID), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("trainingPath.name"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("Should return the curriculum of a selected academic offer")
  void get_returnsSelectedOfferDetail() throws Exception {
    when(getAcademicOfferUseCase.execute(INSTITUTION_ID, STUDY_PLAN_ID))
        .thenReturn(new AcademicOfferDetailResponse(offer(), List.of(), List.of()));

    mockMvc
        .perform(
            get("/api/v1/institutions/" + INSTITUTION_ID + "/academic-offers/" + STUDY_PLAN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offer.trainingPathName").value("CAVI"))
        .andExpect(jsonPath("$.levels").isArray())
        .andExpect(jsonPath("$.unassignedSpaces").isArray());

    verify(getAcademicOfferUseCase).execute(INSTITUTION_ID, STUDY_PLAN_ID);
  }

  private static AcademicOfferSummaryResponse offer() {
    return new AcademicOfferSummaryResponse(
        STUDY_PLAN_ID,
        "Plan CAVI 2026",
        1,
        LocalDate.of(2026, 1, 1),
        null,
        UUID.fromString("66666666-6666-6666-6666-666666666666"),
        "CAVI",
        "Ciclo artístico vocacional infantil");
  }
}
