package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.ActiveSessionResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetActiveSessionsUseCase {

  private final UserSessionRepository userSessionRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<ActiveSessionResponse> execute(
      JwtAuthenticatedUser current, Pageable pageable) {
    var sessions = userSessionRepository.findByUserIdAndActive(current.userId(), true, pageable);
    return PaginatedResponse.from(sessions.map(s -> toResponse(s, current)));
  }

  private static ActiveSessionResponse toResponse(
      UserSession session, JwtAuthenticatedUser current) {
    return new ActiveSessionResponse(
        session.getId(),
        session.getIpAddress(),
        session.getUserAgent(),
        session.getStartedAt(),
        session.getId().equals(current.sessionId()));
  }
}
