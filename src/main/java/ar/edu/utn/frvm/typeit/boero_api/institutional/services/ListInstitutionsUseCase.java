package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListInstitutionsUseCase {

  private final InstitutionRepository institutionRepository;

  public PaginatedResponse<InstitutionListItemResponse> execute(Pageable pageable) {
    return PaginatedResponse.from(
        institutionRepository.findAllWithLocation(pageable).map(InstitutionListItemResponse::from));
  }
}
