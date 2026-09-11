package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PasskeyCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PasskeyUserCredentialRepository implements UserCredentialRepository {

  private final PasskeyCredentialRepository passkeyCredentialRepository;
  private final UserRepository userRepository;
  private final PasskeyCredentialMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public CredentialRecord findByCredentialId(final Bytes credentialId) {
    if (credentialId == null) {
      return null;
    }
    return passkeyCredentialRepository
        .findByCredentialId(credentialId.toBase64UrlString())
        .filter(PasskeyCredential::isActive)
        .map(mapper::toRecord)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CredentialRecord> findByUserId(final Bytes userId) {
    if (userId == null) {
      return List.of();
    }
    return userRepository
        .findByWebauthnUserHandle(userId.getBytes())
        .map(
            user ->
                passkeyCredentialRepository.findActiveByUserId(user.getId()).stream()
                    .map(mapper::toRecord)
                    .toList())
        .orElse(List.of());
  }

  @Override
  @Transactional
  public void save(final CredentialRecord credentialRecord) {
    if (credentialRecord == null || credentialRecord.getCredentialId() == null) {
      return;
    }
    passkeyCredentialRepository
        .findByCredentialId(credentialRecord.getCredentialId().toBase64UrlString())
        .filter(ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential::isActive)
        .ifPresent(
            existing -> {
              existing.markUsed(
                  java.time.LocalDateTime.now(), credentialRecord.getSignatureCount());
              existing.applyTransports(
                  credentialRecord.getTransports() == null
                      ? java.util.Set.of()
                      : credentialRecord.getTransports().stream()
                          .map(transport -> transport == null ? "" : transport.getValue())
                          .collect(java.util.stream.Collectors.toUnmodifiableSet()));
              passkeyCredentialRepository.save(existing);
            });
  }

  @Override
  public void delete(final Bytes credentialId) {}
}
