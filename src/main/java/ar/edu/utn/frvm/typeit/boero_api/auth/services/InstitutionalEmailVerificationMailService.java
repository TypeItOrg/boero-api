package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.EmailVerificationProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.events.InstitutionalEmailVerificationRequested;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailMessage;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailMessages;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailProperties;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailSender;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@NullMarked
public class InstitutionalEmailVerificationMailService {

  private static final String LOGO_URL = "https://i.ibb.co/RGwrB7B8/boero-logo.png";
  private static final String TEMPLATE_NAME = "mail/institutional-email-verification";

  private final MailSender mailSender;
  private final MailProperties mailProperties;
  private final EmailVerificationProperties emailVerificationProperties;
  private final SpringTemplateEngine templateEngine;

  public void send(final InstitutionalEmailVerificationRequested event) {
    final Context context = new Context(Locale.forLanguageTag("es"));
    context.setVariable("verificationUrl", buildVerificationUrl(event.token()));
    context.setVariable("logoUrl", LOGO_URL);
    context.setVariable("institutionName", event.institutionName());
    context.setVariable("fullName", event.fullName());
    context.setVariable("expirationHours", emailVerificationProperties.tokenExpiration().toHours());

    mailSender.send(
        new MailMessage(
            mailProperties.from(),
            event.recipientEmail(),
            MailMessages.EMAIL_VERIFICATION_SUBJECT,
            templateEngine.process(TEMPLATE_NAME, context)));
  }

  private String buildVerificationUrl(final String token) {
    return UriComponentsBuilder.fromUriString(emailVerificationProperties.frontendUrl())
        .pathSegment("auth", "email-verification", "confirm")
        .queryParam("token", token)
        .build()
        .toUriString();
  }
}
