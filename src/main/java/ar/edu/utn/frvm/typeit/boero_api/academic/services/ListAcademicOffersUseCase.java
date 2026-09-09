package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicOfferSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAcademicOffersUseCase {
  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private final StudyPlanRepository studyPlanRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicOfferSummaryResponse> execute(
      final UUID institutionId, final Pageable pageable) {
    return PaginatedResponse.from(
        studyPlanRepository
            .findAvailableOffers(institutionId, LocalDate.now(ARGENTINA_TIME_ZONE), pageable)
            .map(AcademicOfferSummaryResponse::from));
  }
}
