package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.LoginAccountNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.LoginStateInconsistentException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PasskeyCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.IdentifyLoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.IdentifyLoginResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.IdentifyLoginResponse.LoginNextStep;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifyLoginUseCase {

  private final UserRepository userRepository;
  private final PasskeyCredentialRepository passkeyCredentialRepository;
  private final LoginAttemptService loginAttemptService;
  private final AuthRateLimitService rateLimitService;
  private final AuthRateLimitProperties rateLimitProperties;

  @Transactional(readOnly = true)
  public IdentifyLoginResponse execute(
      final IdentifyLoginRequest request, final HttpServletRequest httpRequest) {
    final String normalizedDocument =
        AuthRateLimitService.normalizeDocument(request.documentNumber());
    final String clientIp = AuthRequestMetadata.clientIp(httpRequest);
    rateLimitService.checkAllowed(
        "identify-ip",
        rateLimitService.hashKey(clientIp),
        rateLimitProperties.identifyIpMax(),
        rateLimitProperties.identifyIpWindow());
    rateLimitService.checkAllowed(
        "identify-account",
        rateLimitService.hashKey(request.institutionId() + "|" + normalizedDocument),
        rateLimitProperties.identifyAccountMax(),
        rateLimitProperties.identifyAccountWindow());

    final List<User> users =
        userRepository.findAllByPersonDocumentNumberAndInstitution_Id(
            normalizedDocument, request.institutionId());
    if (users.isEmpty()) {
      log.info(
          "[Auth] Login identification account not found, institutionId: {}",
          request.institutionId());
      throw new LoginAccountNotFoundException();
    }
    if (users.size() > 1) {
      log.error(
          "[Auth] Login identification found multiple users, institutionId: {}, count: {}",
          request.institutionId(),
          users.size());
      throw new LoginStateInconsistentException();
    }
    final User user = users.get(0);
    final boolean hasActivePasskeys =
        passkeyCredentialRepository.existsActiveByUserId(user.getId());
    final LoginAttempt attempt =
        loginAttemptService.create(user.getId(), user.getInstitutionId(), hasActivePasskeys);
    final LoginNextStep nextStep =
        hasActivePasskeys ? LoginNextStep.PASSKEY : LoginNextStep.PASSWORD;
    return new IdentifyLoginResponse(attempt.id(), nextStep);
  }
}
