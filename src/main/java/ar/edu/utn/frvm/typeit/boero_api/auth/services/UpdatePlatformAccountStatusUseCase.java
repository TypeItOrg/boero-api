package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.LastPlatformAdminDisableException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PlatformAccountNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PlatformAccountSelfDisableException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePlatformAccountStatusUseCase {

  private final PlatformAccountRepository platformAccountRepository;
  private final PlatformAccountRoleRepository platformAccountRoleRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(final UUID id, final UUID currentAccountId, final boolean enabled) {
    final PlatformAccount account = findLockedAccount(id);
    if (account.isEnabled() == enabled) {
      return;
    }

    if (!enabled) {
      validateDisable(account, currentAccountId);
    }

    account.setEnabled(enabled);
    platformAccountRepository.save(account);

    if (!enabled) {
      sessionRevocationService.revokePlatformAccountSessions(account.getId());
    }
  }

  private PlatformAccount findLockedAccount(final UUID id) {
    return platformAccountRepository.findAllForUpdate().stream()
        .filter(account -> account.getId().equals(id))
        .findFirst()
        .orElseThrow(PlatformAccountNotFoundException::new);
  }

  private void validateDisable(final PlatformAccount account, final UUID currentAccountId) {
    if (account.getId().equals(currentAccountId)) {
      throw new PlatformAccountSelfDisableException();
    }

    final long enabledAdministrators =
        platformAccountRoleRepository.countEnabledAccountsByRoleCode(
            PlatformRoleCode.PLATFORM_ADMIN.name());
    if (enabledAdministrators <= 1) {
      throw new LastPlatformAdminDisableException();
    }
  }
}
