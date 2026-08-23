package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailMessage;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailMessages;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailProperties;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailSender;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class InstitutionalPasswordRecoveryMailService {

  private static final String LOGO_PATH = "/boero-logo.png";
  private static final String TEMPLATE_NAME = "mail/institutional-password-recovery";

  private final MailSender mailSender;
  private final MailProperties mailProperties;
  private final PasswordRecoveryProperties passwordRecoveryProperties;
  private final SpringTemplateEngine templateEngine;

  public void send(final User user, final String token) {
    final Context context = new Context(Locale.forLanguageTag("es"));
    context.setVariable("resetUrl", buildResetUrl(token));
    context.setVariable("logoUrl", buildLogoUrl());
    context.setVariable("institutionName", user.getInstitution().getName());
    context.setVariable("fullName", user.getName() + " " + user.getLastName());
    context.setVariable(
        "expirationMinutes", passwordRecoveryProperties.tokenExpiration().toMinutes());

    mailSender.send(
        new MailMessage(
            mailProperties.from(),
            user.getPerson().getEmail(),
            MailMessages.PASSWORD_RECOVERY_SUBJECT,
            templateEngine.process(TEMPLATE_NAME, context)));
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
