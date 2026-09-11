package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReAuthenticateUseCase {

  private final UserRepository userRepository;
  private final CredentialsAuthenticator credentialsAuthenticator;
  private final RecentAuthService recentAuthService;
  private final AuthRateLimitService rateLimitService;
  private final AuthRateLimitProperties rateLimitProperties;

  @Transactional(readOnly = true)
  public void execute(
      final JwtAuthenticatedUser principal,
      final String password,
      final HttpServletRequest httpRequest) {
    rateLimitService.checkAllowed(
        "re-auth-ip",
        rateLimitService.hashKey(AuthRequestMetadata.clientIp(httpRequest)),
        rateLimitProperties.reAuthIpMax(),
        rateLimitProperties.reAuthIpWindow());
    rateLimitService.checkAllowed(
        "re-auth",
        rateLimitService.hashKey(principal.userId().toString()),
        rateLimitProperties.reAuthMax(),
        rateLimitProperties.reAuthWindow());
    final User user =
        userRepository
            .findWithPersonAndInstitutionById(principal.userId())
            .orElseThrow(InvalidCredentialsException::new);
    final String username =
        InstitutionalUsername.format(user.getInstitutionId(), user.getDocumentNumber());
    credentialsAuthenticator.authenticate(username, password);
    recentAuthService.mark(
        principal.sessionId(), principal.userId(), AuthenticationSessionIssuer.METHOD_PASSWORD);
    log.info("[Auth] Re-authentication succeeded, userId: {}", principal.userId());
  }
}
