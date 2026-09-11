package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidLoginAttemptException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.WebAuthnCeremonyInvalidException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.WebAuthnVerificationFailedException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PasskeyCredentialRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.webauthn.WebAuthnOptionsCodec;
import com.webauthn4j.verifier.exception.MaliciousCounterValueException;
import com.webauthn4j.verifier.exception.VerificationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyPasskeyAuthenticationUseCase {

  private final LoginAttemptService loginAttemptService;
  private final UserRepository userRepository;
  private final PasskeyCredentialRepository passkeyCredentialRepository;
  private final WebAuthnRelyingPartyOperations relyingPartyOperations;
  private final WebAuthnCeremonyService ceremonyService;
  private final AuthenticationSessionIssuer sessionIssuer;
  private final AuthRateLimitService rateLimitService;
  private final AuthRateLimitProperties rateLimitProperties;
  private final WebAuthnOptionsCodec optionsCodec;

  @Transactional
  public AuthResponse execute(
      final String loginAttemptId,
      final String ceremonyId,
      final JsonNode credentialNode,
      final Boolean rememberMe,
      final HttpServletRequest httpRequest) {
    final LoginAttempt attempt = loginAttemptService.resolve(loginAttemptId);
    rateLimitService.checkAllowed(
        "passkey-verify-ip",
        rateLimitService.hashKey(AuthRequestMetadata.clientIp(httpRequest)),
        rateLimitProperties.webauthnIpMax(),
        rateLimitProperties.webauthnIpWindow());
    rateLimitService.checkAllowed(
        "passkey-verify",
        rateLimitService.hashKey(attempt.userId().toString()),
        rateLimitProperties.webauthnMax(),
        rateLimitProperties.webauthnWindow());
    final AuthenticationCeremony ceremony =
        ceremonyService.consumeAuthentication(ceremonyId).orElse(null);
    if (ceremony == null) {
      log.info("[Auth] Passkey auth ceremony missing, userId: {}", attempt.userId());
      throw new WebAuthnCeremonyInvalidException();
    }
    final boolean boundToAttempt =
        ceremony.loginAttemptId().equals(attempt.id())
            && ceremony.userId().equals(attempt.userId());
    if (!boundToAttempt) {
      log.info("[Auth] Passkey auth ceremony not bound, userId: {}", attempt.userId());
      throw new WebAuthnCeremonyInvalidException();
    }
    final PublicKeyCredentialRequestOptions options = readOptions(ceremony.optionsJson());
    final PublicKeyCredential<AuthenticatorAssertionResponse> credential =
        readCredential(credentialNode);
    final PublicKeyCredentialUserEntity owner;
    try {
      owner =
          relyingPartyOperations.authenticate(
              new RelyingPartyAuthenticationRequest(options, credential));
    } catch (final MaliciousCounterValueException exception) {
      log.warn("[Auth] Passkey auth counter mismatch, userId: {}", attempt.userId());
      throw new WebAuthnVerificationFailedException();
    } catch (final VerificationException | IllegalArgumentException exception) {
      log.info("[Auth] Passkey auth failed, userId: {}", attempt.userId());
      throw new WebAuthnVerificationFailedException();
    }
    final User expected =
        userRepository
            .findWithPersonAndInstitutionById(attempt.userId())
            .orElseThrow(InvalidLoginAttemptException::new);
    final boolean ownerMatches =
        owner != null
            && owner.getId() != null
            && expected.getWebauthnUserHandle() != null
            && java.util.Arrays.equals(owner.getId().getBytes(), expected.getWebauthnUserHandle());
    if (!ownerMatches) {
      log.info("[Auth] Passkey auth owner mismatch, userId: {}", attempt.userId());
      throw new WebAuthnVerificationFailedException();
    }
    if (!expected.isEnabled()) {
      throw new WebAuthnVerificationFailedException();
    }
    final String credentialId = credential.getId();
    final PasskeyCredential stored =
        passkeyCredentialRepository
            .findByCredentialId(credentialId)
            .filter(PasskeyCredential::isActive)
            .orElseThrow(WebAuthnVerificationFailedException::new);
    if (!stored.getUser().getId().equals(expected.getId())) {
      throw new WebAuthnVerificationFailedException();
    }
    loginAttemptService.claim(attempt.id());
    final boolean remember = Boolean.TRUE.equals(rememberMe);
    final AuthResponse response =
        sessionIssuer.issue(
            expected,
            AuthRequestMetadata.clientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            remember,
            AuthenticationSessionIssuer.METHOD_PASSKEY);
    log.info("[Auth] Passkey auth succeeded, userId: {}", expected.getId());
    return response;
  }

  private PublicKeyCredentialRequestOptions readOptions(final String json) {
    final tools.jackson.databind.JsonNode snapshot = parseSnapshot(json);
    try {
      return optionsCodec.rebuildRequestOptions(snapshot);
    } catch (IllegalStateException exception) {
      log.info(
          "[Auth] Failed to reconstruct Spring WebAuthn authentication options, exceptionType={}, message={}",
          exceptionType(exception),
          exceptionMessage(exception));
      throw new WebAuthnCeremonyInvalidException();
    }
  }

  private tools.jackson.databind.JsonNode parseSnapshot(final String json) {
    try {
      return optionsCodec.parseSnapshot(json);
    } catch (IllegalStateException exception) {
      log.info(
          "[Auth] Failed to deserialize WebAuthn authentication snapshot, exceptionType={}, message={}",
          exceptionType(exception),
          exceptionMessage(exception));
      throw new WebAuthnCeremonyInvalidException();
    }
  }

  private static String exceptionType(final IllegalStateException exception) {
    if (exception.getCause() == null) {
      return exception.getClass().getSimpleName();
    }
    return exception.getCause().getClass().getSimpleName();
  }

  private static String exceptionMessage(final IllegalStateException exception) {
    if (exception.getCause() instanceof IllegalArgumentException cause) {
      return cause.getMessage();
    }
    return exception.getMessage();
  }

  private PublicKeyCredential<AuthenticatorAssertionResponse> readCredential(final JsonNode node) {
    try {
      return optionsCodec.decodeAssertionCredential(node);
    } catch (IllegalStateException exception) {
      log.info("[Auth] Passkey auth credential unreadable");
      throw new WebAuthnVerificationFailedException();
    }
  }
}
