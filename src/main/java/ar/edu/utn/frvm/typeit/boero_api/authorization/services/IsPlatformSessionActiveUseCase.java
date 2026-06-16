package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsPlatformSessionActiveUseCase {

  private final PlatformSessionRepository platformSessionRepository;

  public boolean execute(UUID sessionId) {
    return platformSessionRepository.existsByIdAndActiveTrue(sessionId);
  }
}
