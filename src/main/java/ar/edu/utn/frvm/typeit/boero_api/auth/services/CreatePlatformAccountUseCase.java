package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PlatformAccountEmailAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.CreatePlatformAccountRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAccountAdminResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AssignPlatformRoleUseCase;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePlatformAccountUseCase {

  private final PlatformAccountRepository platformAccountRepository;
  private final AssignPlatformRoleUseCase assignPlatformRoleUseCase;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public PlatformAccountAdminResponse execute(final CreatePlatformAccountRequest request) {
    final String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (platformAccountRepository.existsByEmailIgnoreCase(email)) {
      throw new PlatformAccountEmailAlreadyExistsException();
    }

    final PlatformAccount account = saveAccount(request, email);
    assignPlatformRoleUseCase.execute(account, PlatformRoleCode.PLATFORM_ADMIN, false);

    return PlatformAccountAdminResponse.from(account);
  }

  private PlatformAccount saveAccount(
      final CreatePlatformAccountRequest request, final String email) {
    try {
      return platformAccountRepository.saveAndFlush(
          PlatformAccount.builder()
              .name(request.name().trim())
              .lastName(request.lastName().trim())
              .email(email)
              .password(passwordEncoder.encode(request.password()))
              .enabled(true)
              .build());
    } catch (final DataIntegrityViolationException exception) {
      throw new PlatformAccountEmailAlreadyExistsException();
    }
  }
}
