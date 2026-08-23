package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.SHA_256_UNAVAILABLE;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.events.InstitutionalPasswordRecoveryRequested;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PasswordRecoveryRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestInstitutionalPasswordRecoveryUseCase {

  private static final int TOKEN_BYTES = 32;

  private final UserRepository userRepository;
  private final InstitutionalPasswordResetTokenRepository passwordResetTokenRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final PasswordRecoveryProperties passwordRecoveryProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public void execute(final PasswordRecoveryRequest request) {
    userRepository
        .findWithPersonAndInstitutionForPasswordRecovery(
            request.documentNumber(), request.institutionId())
        .filter(this::canRecoverPassword)
        .ifPresent(this::createTokenAndPublishEvent);
  }

  private boolean canRecoverPassword(final User user) {
    return user.isEnabled()
        && user.getPerson().getEmail() != null
        && !user.getPerson().getEmail().isBlank();
  }

  private void createTokenAndPublishEvent(final User user) {
    final String token = generateToken();
    passwordResetTokenRepository.deleteByUserId(user.getId());
    passwordResetTokenRepository.save(
        InstitutionalPasswordResetToken.builder()
            .user(user)
            .tokenHash(hash(token))
            .expiresAt(LocalDateTime.now().plus(passwordRecoveryProperties.tokenExpiration()))
            .build());

    eventPublisher.publishEvent(InstitutionalPasswordRecoveryRequested.from(user, token));
  }

  private String generateToken() {
    final byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(final String token) {
    try {
      return Base64.getEncoder()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (final NoSuchAlgorithmException exception) {
      throw new IllegalStateException(SHA_256_UNAVAILABLE, exception);
    }
  }
}
