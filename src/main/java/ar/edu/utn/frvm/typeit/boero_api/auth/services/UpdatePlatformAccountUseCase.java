package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PlatformAccountEmailAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PlatformAccountNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.UpdatePlatformAccountRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAccountAdminResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePlatformAccountUseCase {

  private final PlatformAccountRepository platformAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public PlatformAccountAdminResponse execute(
      final UUID id, final UpdatePlatformAccountRequest request) {
    final PlatformAccount account =
        platformAccountRepository.findById(id).orElseThrow(PlatformAccountNotFoundException::new);
    final String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (platformAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
      throw new PlatformAccountEmailAlreadyExistsException();
    }

    final boolean emailChanged = !account.getEmail().equals(email);
    final boolean passwordChanged = request.password() != null && !request.password().isEmpty();

    account.setName(request.name().trim());
    account.setLastName(request.lastName().trim());
    account.setEmail(email);
    if (passwordChanged) {
      account.setPassword(passwordEncoder.encode(request.password()));
    }

    saveAccount(account);

    if (emailChanged || passwordChanged) {
      sessionRevocationService.revokePlatformAccountSessions(id);
    }

    return PlatformAccountAdminResponse.from(account);
  }

  private void saveAccount(final PlatformAccount account) {
    try {
      platformAccountRepository.saveAndFlush(account);
    } catch (final DataIntegrityViolationException exception) {
      throw new PlatformAccountEmailAlreadyExistsException();
    }
  }
}
