package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidPasswordRecoveryTokenException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PasswordConfirmationMismatchException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.ResetPasswordRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetInstitutionalPasswordUseCase {

  private final InstitutionalPasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(final ResetPasswordRequest request) {
    if (!request.password().equals(request.confirmPassword())) {
      throw new PasswordConfirmationMismatchException();
    }

    final LocalDateTime now = LocalDateTime.now();
    final InstitutionalPasswordResetToken token =
        passwordResetTokenRepository
            .findByTokenHashForUpdate(
                RequestInstitutionalPasswordRecoveryUseCase.hash(request.token()))
            .filter(resetToken -> resetToken.isUsableAt(now))
            .orElseThrow(InvalidPasswordRecoveryTokenException::new);

    token.getUser().changePassword(passwordEncoder.encode(request.password()));
    token.markUsed(now);
    sessionRevocationService.revokeInstitutionalSessionsForUser(token.getUser().getId());
  }
}
