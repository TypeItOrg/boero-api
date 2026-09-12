package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CredentialsAuthenticatorTest {

  @Mock private AuthenticationManager authenticationManager;

  @Test
  @DisplayName("Should map bad credentials to a generic failure")
  void authenticate_mapsBadCredentials() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    final CredentialsAuthenticator authenticator =
        new CredentialsAuthenticator(authenticationManager);

    assertThatThrownBy(() -> authenticator.authenticate("user", "wrong"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Should map a disabled account to the same generic failure")
  void authenticate_mapsDisabledAccountWithoutLeakingState() {
    when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
    final CredentialsAuthenticator authenticator =
        new CredentialsAuthenticator(authenticationManager);

    assertThatThrownBy(() -> authenticator.authenticate("user", "secret"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Should return the authentication on success")
  void authenticate_returnsAuthentication() {
    final var expected = UsernamePasswordAuthenticationToken.authenticated("user", null, List.of());
    when(authenticationManager.authenticate(any())).thenReturn(expected);
    final CredentialsAuthenticator authenticator =
        new CredentialsAuthenticator(authenticationManager);

    assertThat(authenticator.authenticate("user", "secret")).isSameAs(expected);
  }
}
