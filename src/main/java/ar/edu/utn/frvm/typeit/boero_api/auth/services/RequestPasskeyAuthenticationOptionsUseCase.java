package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidLoginAttemptException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PasskeyAuthenticationOptionsResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.webauthn.WebAuthnOptionsCodec;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestPasskeyAuthenticationOptionsUseCase {

  private final LoginAttemptService loginAttemptService;
  private final UserRepository userRepository;
  private final WebAuthnRelyingPartyOperations relyingPartyOperations;
  private final WebAuthnCeremonyService ceremonyService;
  private final AuthRateLimitService rateLimitService;
  private final AuthRateLimitProperties rateLimitProperties;
  private final WebAuthnOptionsCodec optionsCodec;

  @Transactional(readOnly = true)
  public PasskeyAuthenticationOptionsResponse execute(
      final String loginAttemptId, final HttpServletRequest httpRequest) {
    final LoginAttempt attempt = loginAttemptService.resolve(loginAttemptId);
    if (!attempt.hasActivePasskeys()) {
      throw new InvalidLoginAttemptException();
    }
    rateLimitService.checkAllowed(
        "passkey-options-ip",
        rateLimitService.hashKey(AuthRequestMetadata.clientIp(httpRequest)),
        rateLimitProperties.webauthnIpMax(),
        rateLimitProperties.webauthnIpWindow());
    rateLimitService.checkAllowed(
        "passkey-options",
        rateLimitService.hashKey(attempt.userId().toString()),
        rateLimitProperties.webauthnMax(),
        rateLimitProperties.webauthnWindow());
    final User user =
        userRepository
            .findWithPersonAndInstitutionById(attempt.userId())
            .orElseThrow(InvalidLoginAttemptException::new);
    final Authentication authentication =
        UsernamePasswordAuthenticationToken.unauthenticated(user.getId().toString(), null);
    final PublicKeyCredentialRequestOptions options =
        relyingPartyOperations.createCredentialRequestOptions(
            new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication));
    final String optionsJson = optionsCodec.encodeRequestOptions(options);
    final String ceremonyId =
        ceremonyService.storeAuthentication(attempt.id(), user.getId(), optionsJson);
    return new PasskeyAuthenticationOptionsResponse(
        ceremonyId, optionsCodec.requestOptionsTree(options));
  }
}
