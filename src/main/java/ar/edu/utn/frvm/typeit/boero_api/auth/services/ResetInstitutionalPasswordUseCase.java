package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidPasswordRecoveryTokenException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PasswordConfirmationMismatchException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.ResetPasswordRequest;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetInstitutionalPasswordUseCase {
  private final Clock clock;
  private final UserRepository userRepository;

  private final InstitutionalPasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(final ResetPasswordRequest request) {
    if (!request.password().equals(request.confirmPassword())) {
      throw new PasswordConfirmationMismatchException();
    }

    final String hash = RequestInstitutionalPasswordRecoveryUseCase.hash(request.token());
    passwordResetTokenRepository
        .findUserIdByTokenHash(hash)
        .flatMap(userRepository::findForEmailVerificationById)
        .filter(User::isAccountActive)
        .orElseThrow(InvalidPasswordRecoveryTokenException::new);
    final Instant now = clock.instant();
    final InstitutionalPasswordResetToken token =
        passwordResetTokenRepository
            .findByTokenHashForUpdate(hash)
            .filter(resetToken -> resetToken.isUsableAt(now))
            .orElseThrow(InvalidPasswordRecoveryTokenException::new);

    token.getUser().changePassword(passwordEncoder.encode(request.password()));
    token.markUsed(now);
    sessionRevocationService.revokeInstitutionalSessionsForUser(token.getUser().getId());
  }
}
