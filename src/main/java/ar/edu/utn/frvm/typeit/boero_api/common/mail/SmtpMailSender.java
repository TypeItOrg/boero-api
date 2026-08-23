package ar.edu.utn.frvm.typeit.boero_api.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

  private final JavaMailSender javaMailSender;

  @Override
  public void send(final MailMessage mailMessage) {
    final MimeMessage message = javaMailSender.createMimeMessage();
    try {
      final MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(mailMessage.from());
      helper.setTo(mailMessage.to());
      helper.setSubject(mailMessage.subject());
      helper.setText(mailMessage.htmlBody(), true);
      javaMailSender.send(message);
    } catch (MessagingException | MailException exception) {
      throw new MailSendingException(exception);
    }
  }
}
