package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.InstrumentResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListInstrumentsUseCase {
  private final InstrumentRepository instrumentRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<InstrumentResponse> execute(
      final UUID institutionId,
      final String search,
      final Boolean active,
      final Pageable pageable) {
    return PaginatedResponse.from(
        instrumentRepository
            .findByFilters(institutionId, AcademicNameNormalizer.search(search), active, pageable)
            .map(InstrumentResponse::from));
  }
}
