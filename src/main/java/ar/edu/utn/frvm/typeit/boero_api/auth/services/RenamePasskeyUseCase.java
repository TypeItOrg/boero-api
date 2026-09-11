package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PasskeyNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PasskeyCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PasskeyResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RenamePasskeyUseCase {

  private final PasskeyCredentialRepository passkeyCredentialRepository;

  @Transactional
  public PasskeyResponse execute(
      final JwtAuthenticatedUser principal, final UUID passkeyId, final String label) {
    final var credential =
        passkeyCredentialRepository
            .findByIdAndUserId(passkeyId, principal.userId())
            .filter(c -> c.isActive())
            .orElseThrow(PasskeyNotFoundException::new);
    credential.rename(label);
    final var saved = passkeyCredentialRepository.save(credential);
    log.info("[Auth] Passkey renamed, userId: {}", principal.userId());
    return PasskeyResponse.from(saved);
  }
}
