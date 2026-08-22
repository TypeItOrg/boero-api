package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.MailProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstitutionalPasswordRecoveryMailService {

  private static final String LOGO_PATH = "/boero-logo.png";

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final PasswordRecoveryProperties passwordRecoveryProperties;

  public void send(final User user, final String token) {
    final String resetUrl = buildResetUrl(token);
    final MimeMessage message = mailSender.createMimeMessage();
    try {
      final MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(mailProperties.from());
      helper.setTo(user.getPerson().getEmail());
      helper.setSubject("Recuperación de contraseña");
      helper.setText(buildEmailBody(resetUrl), true);
      mailSender.send(message);
    } catch (MessagingException exception) {
      throw new MailPreparationException(
          "No se pudo preparar el correo de recuperación.", exception);
    }
  }

  private String buildEmailBody(final String resetUrl) {
    final long expirationMinutes = passwordRecoveryProperties.tokenExpiration().toMinutes();
    final String logoUrl = buildLogoUrl();
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;background:#fff8f2;font-family:Arial,Helvetica,sans-serif;color:#2e1d20">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:36px 16px">
              <tr><td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #eadfda;border-radius:20px;overflow:hidden">
                  <tr><td align="center" style="padding:28px 32px 24px;background:#76002f;border-bottom:5px solid #f4a900">
                    <img src="%s" width="82" alt="Conservatorio Superior de Música Felipe Boero" style="display:block;width:82px;height:82px;border:0;border-radius:50%%" />
                    <p style="margin:14px 0 0;color:#fff8f2;font-size:13px;font-weight:bold;letter-spacing:0.7px;text-transform:uppercase">Conservatorio Superior de Música</p>
                    <p style="margin:4px 0 0;color:#fbd778;font-size:20px;font-weight:bold">Felipe Boero</p>
                  </td></tr>
                  <tr><td style="padding:34px 32px 28px">
                    <h1 style="margin:0 0 16px;font-size:25px;line-height:1.25;color:#76002f">Restablecé tu contraseña</h1>
                    <p style="margin:0 0 22px;font-size:16px;line-height:1.55">Recibimos una solicitud para cambiar la contraseña de tu cuenta institucional.</p>
                    <p style="margin:0 0 26px"><a href="%s" style="display:inline-block;background:#76002f;border:2px solid #76002f;border-radius:10px;color:#ffffff;padding:13px 22px;text-decoration:none;font-size:16px;font-weight:bold">Restablecer contraseña</a></p>
                    <div style="margin:0 0 22px;padding:14px 16px;background:#fff3da;border-left:4px solid #f4a900;border-radius:8px;font-size:14px;line-height:1.5">
                      Este enlace vence en <strong>%d minutos</strong> y puede usarse una sola vez.
                    </div>
                    <p style="margin:0;font-size:14px;line-height:1.5;color:#765e61">Si no solicitaste este cambio, podés ignorar este correo. Tu contraseña seguirá siendo la misma.</p>
                  </td></tr>
                  <tr><td align="center" style="padding:18px 24px;background:#fff8f2;color:#765e61;font-size:12px">Boero · Gestión institucional</td></tr>
                </table>
              </td></tr>
            </table>
          </body>
        </html>
        """
        .formatted(logoUrl, resetUrl, expirationMinutes);
  }

  private String buildLogoUrl() {
    return URI.create(passwordRecoveryProperties.frontendUrl()).resolve(LOGO_PATH).toString();
  }

  private String buildResetUrl(final String token) {
    final URI frontendUri = URI.create(passwordRecoveryProperties.frontendUrl());
    final String separator = frontendUri.getQuery() == null ? "?" : "&";
    return frontendUri.toString().replaceAll("/$", "")
        + "/auth/password-recovery/reset"
        + separator
        + "token="
        + token;
  }
}
