package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsSessionActiveUseCase {

  private final UserSessionRepository userSessionRepository;

  @org.springframework.cache.annotation.Cacheable(value = "activeSessions", key = "#sessionId")
  public boolean execute(UUID sessionId) {
    return userSessionRepository.existsUsableById(sessionId);
  }
}
