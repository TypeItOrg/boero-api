package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAcademicSpacesUseCase {
  private final AcademicSpaceRepository academicSpaceRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicSpaceResponse> execute(
      final UUID institutionId,
      final String search,
      final Boolean active,
      final AcademicSpaceType type,
      final Pageable pageable) {
    return PaginatedResponse.from(
        academicSpaceRepository
            .findByFilters(
                institutionId, AcademicNameNormalizer.search(search), active, type, pageable)
            .map(AcademicSpaceResponse::from));
  }
}
