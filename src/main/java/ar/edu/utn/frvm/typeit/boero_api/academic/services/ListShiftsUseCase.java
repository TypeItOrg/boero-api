package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.ShiftRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ShiftResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListShiftsUseCase {
  private final ShiftRepository shiftRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<ShiftResponse> execute(
      final UUID institutionId,
      final String search,
      final Boolean active,
      final boolean deleted,
      final Pageable pageable) {
    return PaginatedResponse.from(
        shiftRepository
            .findByFilters(
                institutionId, AcademicNameNormalizer.search(search), active, deleted, pageable)
            .map(ShiftResponse::from));
  }

  public PaginatedResponse<ShiftResponse> execute(
      final UUID institutionId,
      final String search,
      final Boolean active,
      final Pageable pageable) {
    return execute(institutionId, search, active, false, pageable);
  }
}
