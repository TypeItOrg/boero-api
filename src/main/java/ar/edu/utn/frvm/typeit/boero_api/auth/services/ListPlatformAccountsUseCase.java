package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAccountAdminResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPlatformAccountsUseCase {

  private final PlatformAccountRepository platformAccountRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<PlatformAccountAdminResponse> execute(
      final String search, final Boolean enabled, final Pageable pageable) {
    final String normalizedSearch = normalizeSearch(search);
    final var page =
        platformAccountRepository
            .findByFilters(normalizedSearch, enabled, pageable)
            .map(PlatformAccountAdminResponse::from);

    return PaginatedResponse.from(page);
  }

  private static String normalizeSearch(final String search) {
    if (search == null || search.isBlank()) {
      return null;
    }

    return search.trim();
  }
}
