package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PasskeyCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;

@ExtendWith(MockitoExtension.class)
class PasskeyUserCredentialRepositoryTest {

  @Mock private PasskeyCredentialRepository passkeyCredentialRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasskeyCredentialMapper mapper;

  @Test
  @DisplayName("Should not create credentials on save for unknown ids")
  void save_ignoresUnknownCredentialIds() {
    final PasskeyUserCredentialRepository repository =
        new PasskeyUserCredentialRepository(
            Clock.systemUTC(), passkeyCredentialRepository, userRepository, mapper);
    final CredentialRecord record = Mockito.mock(CredentialRecord.class);
    when(record.getCredentialId()).thenReturn(Bytes.random());
    when(passkeyCredentialRepository.findWithLockByCredentialId(any()))
        .thenReturn(Optional.empty());

    repository.save(record);

    verify(passkeyCredentialRepository, never()).save(any(PasskeyCredential.class));
  }

  @Test
  @DisplayName("Should update usage metadata on save for known credentials")
  void save_updatesKnownCredential() {
    final PasskeyCredential existing = Mockito.mock(PasskeyCredential.class);
    when(existing.isActive()).thenReturn(true);
    when(passkeyCredentialRepository.findWithLockByCredentialId(any()))
        .thenReturn(Optional.of(existing));
    final PasskeyUserCredentialRepository repository =
        new PasskeyUserCredentialRepository(
            Clock.systemUTC(), passkeyCredentialRepository, userRepository, mapper);
    final CredentialRecord record = Mockito.mock(CredentialRecord.class);
    when(record.getCredentialId()).thenReturn(Bytes.random());
    when(record.getSignatureCount()).thenReturn(7L);
    when(record.getTransports()).thenReturn(Set.<AuthenticatorTransport>of());

    repository.save(record);

    verify(existing).markUsed(any(Instant.class), eq(7L));
    verify(passkeyCredentialRepository).save(existing);
  }

  @Test
  @DisplayName("Should resolve active credentials by id without leaking revoked ones")
  void findByCredentialId_filtersRevoked() {
    final PasskeyUserCredentialRepository repository =
        new PasskeyUserCredentialRepository(
            Clock.systemUTC(), passkeyCredentialRepository, userRepository, mapper);
    final PasskeyCredential revoked = Mockito.mock(PasskeyCredential.class);
    when(revoked.isActive()).thenReturn(false);
    when(passkeyCredentialRepository.findWithLockByCredentialId(any()))
        .thenReturn(Optional.of(revoked));

    assertThat(repository.findByCredentialId(Bytes.random())).isNull();
    verify(mapper, never()).toRecord(any());
  }

  @Test
  @DisplayName("Should resolve credentials of a user handle known to the user table")
  void findByUserId_resolvesThroughUserHandle() {
    final UUID userId = UUID.randomUUID();
    final User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(userRepository.findByWebauthnUserHandle(any())).thenReturn(Optional.of(user));
    final PasskeyCredential credential = Mockito.mock(PasskeyCredential.class);
    when(passkeyCredentialRepository.findActiveByUserId(userId)).thenReturn(List.of(credential));
    final CredentialRecord mapped = Mockito.mock(CredentialRecord.class);
    when(mapper.toRecord(credential)).thenReturn(mapped);
    final PasskeyUserCredentialRepository repository =
        new PasskeyUserCredentialRepository(
            Clock.systemUTC(), passkeyCredentialRepository, userRepository, mapper);

    assertThat(repository.findByUserId(Bytes.random())).containsExactly(mapped);
  }
}
