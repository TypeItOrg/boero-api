package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidLoginAttemptException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PasswordLoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordLoginWithAttemptUseCase {

  private final LoginAttemptService loginAttemptService;
  private final UserRepository userRepository;
  private final CredentialsAuthenticator credentialsAuthenticator;
  private final AuthenticationSessionIssuer sessionIssuer;
  private final AuthRateLimitService rateLimitService;
  private final AuthRateLimitProperties rateLimitProperties;

  @Transactional
  public AuthResponse execute(
      final PasswordLoginRequest request, final HttpServletRequest httpRequest) {
    final LoginAttempt attempt = loginAttemptService.resolve(request.loginAttemptId());
    rateLimitService.checkAllowed(
        "login-password-ip",
        rateLimitService.hashKey(AuthRequestMetadata.clientIp(httpRequest)),
        rateLimitProperties.passwordIpMax(),
        rateLimitProperties.passwordIpWindow());
    rateLimitService.checkAllowed(
        "login-password",
        rateLimitService.hashKey(attempt.userId().toString()),
        rateLimitProperties.passwordMax(),
        rateLimitProperties.passwordWindow());

    final User user =
        userRepository
            .findWithPersonAndInstitutionById(attempt.userId())
            .orElseThrow(InvalidLoginAttemptException::new);
    final boolean institutionMatches = user.getInstitutionId().equals(attempt.institutionId());
    if (!institutionMatches) {
      loginAttemptService.invalidate(attempt.id());
      throw new InvalidLoginAttemptException();
    }
    final String principal =
        InstitutionalUsername.format(user.getInstitutionId(), user.getDocumentNumber());
    final Authentication authentication;
    try {
      authentication = credentialsAuthenticator.authenticate(principal, request.password());
    } catch (InvalidCredentialsException exception) {
      throw exception;
    }
    final User authenticated = (User) authentication.getPrincipal();
    if (!authenticated.isEnabled()) {
      throw new InvalidCredentialsException();
    }
    loginAttemptService.claim(attempt.id());
    final boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
    final AuthResponse response =
        sessionIssuer.issue(
            authenticated,
            AuthRequestMetadata.clientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            rememberMe,
            AuthenticationSessionIssuer.METHOD_PASSWORD);
    return response;
  }
}
