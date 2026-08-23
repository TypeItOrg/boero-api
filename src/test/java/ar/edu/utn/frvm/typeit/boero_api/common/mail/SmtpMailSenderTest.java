package ar.edu.utn.frvm.typeit.boero_api.common.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpMailSenderTest {

  @Mock private JavaMailSender javaMailSender;

  @Test
  void sendMapsMailMessageToHtmlMimeMessage() {
    final MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    final SmtpMailSender mailSender = new SmtpMailSender(javaMailSender);

    mailSender.send(
        new MailMessage("no-reply@example.com", "ana@example.com", "Asunto", "<p>Contenido</p>"));

    verify(javaMailSender).send(mimeMessage);
    assertThat(readSubject(mimeMessage)).isEqualTo("Asunto");
    assertThat(readContent(mimeMessage)).contains("<p>Contenido</p>");
  }

  private static String readSubject(final MimeMessage message) {
    try {
      return message.getSubject();
    } catch (MessagingException exception) {
      throw new AssertionError(exception);
    }
  }

  private static String readContent(final MimeMessage message) {
    try {
      return message.getContent().toString();
    } catch (MessagingException | IOException exception) {
      throw new AssertionError(exception);
    }
  }
}
