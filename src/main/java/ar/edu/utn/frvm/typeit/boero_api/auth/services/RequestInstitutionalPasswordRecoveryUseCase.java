package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.SHA_256_UNAVAILABLE;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PasswordRecoveryRequest;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailSendingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestInstitutionalPasswordRecoveryUseCase {

  private static final int TOKEN_BYTES = 32;

  private final UserRepository userRepository;
  private final InstitutionalPasswordResetTokenRepository passwordResetTokenRepository;
  private final InstitutionalPasswordRecoveryMailService mailService;
  private final PasswordRecoveryProperties passwordRecoveryProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public void execute(final PasswordRecoveryRequest request) {
    userRepository
        .findWithPersonAndInstitutionForPasswordRecovery(
            request.documentNumber(), request.institutionId())
        .filter(this::canRecoverPassword)
        .ifPresent(this::createAndSendToken);
  }

  private boolean canRecoverPassword(final User user) {
    return user.isEnabled()
        && user.getPerson().getEmail() != null
        && !user.getPerson().getEmail().isBlank();
  }

  private void createAndSendToken(final User user) {
    final String token = generateToken();
    passwordResetTokenRepository.deleteByUserId(user.getId());
    passwordResetTokenRepository.save(
        InstitutionalPasswordResetToken.builder()
            .user(user)
            .tokenHash(hash(token))
            .expiresAt(LocalDateTime.now().plus(passwordRecoveryProperties.tokenExpiration()))
            .build());

    try {
      mailService.send(user, token);
    } catch (final MailSendingException exception) {
      log.warn("[Auth] Password recovery email could not be sent, userId: {}", user.getId());
    }
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
