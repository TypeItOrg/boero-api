package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.PasswordConfirmationMismatchException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.ResetPasswordRequest;
import java.time.Clock;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class ResetInstitutionalPasswordUseCaseTest {

  @Test
  @DisplayName(
      "Should not consume the token or change the password when confirmation does not match")
  void execute_doesNotChangePasswordWhenConfirmationDoesNotMatch() {
    final InstitutionalPasswordResetTokenRepository passwordResetTokenRepository =
        Mockito.mock(InstitutionalPasswordResetTokenRepository.class);
    final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    final SessionRevocationService sessionRevocationService =
        Mockito.mock(SessionRevocationService.class);
    final AuthRateLimitService rateLimitService = Mockito.mock(AuthRateLimitService.class);
    final AuthRateLimitProperties rateLimitProperties = Mockito.mock(AuthRateLimitProperties.class);
    final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
    final ResetInstitutionalPasswordUseCase useCase =
        new ResetInstitutionalPasswordUseCase(
            Clock.systemUTC(),
            passwordResetTokenRepository,
            passwordEncoder,
            sessionRevocationService,
            rateLimitService,
            rateLimitProperties);
    final ResetPasswordRequest request =
        new ResetPasswordRequest("token", "password123", "different-password");

    assertThatThrownBy(() -> useCase.execute(request, httpRequest))
        .isInstanceOf(PasswordConfirmationMismatchException.class);

    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder, sessionRevocationService);
  }
}
