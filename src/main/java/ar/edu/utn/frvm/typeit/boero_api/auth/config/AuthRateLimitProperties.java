package ar.edu.utn.frvm.typeit.boero_api.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.rate-limit")
public record AuthRateLimitProperties(
    String keySecret,
    int identifyIpMax,
    Duration identifyIpWindow,
    int identifyAccountMax,
    Duration identifyAccountWindow,
    int passwordMax,
    Duration passwordWindow,
    int passwordIpMax,
    Duration passwordIpWindow,
    int webauthnMax,
    Duration webauthnWindow,
    int webauthnIpMax,
    Duration webauthnIpWindow,
    int reAuthMax,
    Duration reAuthWindow,
    int reAuthIpMax,
    Duration reAuthIpWindow,
    int recoveryRequestIpMax,
    Duration recoveryRequestIpWindow,
    int recoveryRequestAccountMax,
    Duration recoveryRequestAccountWindow,
    int passwordResetIpMax,
    Duration passwordResetIpWindow) {

  public AuthRateLimitProperties {
    requireNonBlank(keySecret, "app.auth.rate-limit.key-secret");
    requirePositive(identifyIpMax, "app.auth.rate-limit.identify-ip-max");
    requirePositive(identifyIpWindow, "app.auth.rate-limit.identify-ip-window");
    requirePositive(identifyAccountMax, "app.auth.rate-limit.identify-account-max");
    requirePositive(identifyAccountWindow, "app.auth.rate-limit.identify-account-window");
    requirePositive(passwordMax, "app.auth.rate-limit.password-max");
    requirePositive(passwordWindow, "app.auth.rate-limit.password-window");
    requirePositive(passwordIpMax, "app.auth.rate-limit.password-ip-max");
    requirePositive(passwordIpWindow, "app.auth.rate-limit.password-ip-window");
    requirePositive(webauthnMax, "app.auth.rate-limit.webauthn-max");
    requirePositive(webauthnWindow, "app.auth.rate-limit.webauthn-window");
    requirePositive(webauthnIpMax, "app.auth.rate-limit.webauthn-ip-max");
    requirePositive(webauthnIpWindow, "app.auth.rate-limit.webauthn-ip-window");
    requirePositive(reAuthMax, "app.auth.rate-limit.re-auth-max");
    requirePositive(reAuthWindow, "app.auth.rate-limit.re-auth-window");
    requirePositive(reAuthIpMax, "app.auth.rate-limit.re-auth-ip-max");
    requirePositive(reAuthIpWindow, "app.auth.rate-limit.re-auth-ip-window");
    requirePositive(recoveryRequestIpMax, "app.auth.rate-limit.recovery-request-ip-max");
    requirePositive(recoveryRequestIpWindow, "app.auth.rate-limit.recovery-request-ip-window");
    requirePositive(recoveryRequestAccountMax, "app.auth.rate-limit.recovery-request-account-max");
    requirePositive(
        recoveryRequestAccountWindow, "app.auth.rate-limit.recovery-request-account-window");
    requirePositive(passwordResetIpMax, "app.auth.rate-limit.password-reset-ip-max");
    requirePositive(passwordResetIpWindow, "app.auth.rate-limit.password-reset-ip-window");
  }

  private static void requireNonBlank(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("La propiedad " + name + " es obligatoria.");
    }
  }

  private static void requirePositive(final int value, final String name) {
    if (value <= 0) {
      throw new IllegalArgumentException("La propiedad " + name + " debe ser positiva.");
    }
  }

  private static void requirePositive(final Duration value, final String name) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException("La propiedad " + name + " debe ser positiva.");
    }
  }
}
