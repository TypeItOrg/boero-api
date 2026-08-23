package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.events.InstitutionalPasswordRecoveryRequested;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailMessage;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailProperties;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailSender;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
class InstitutionalPasswordRecoveryMailServiceTest {

  @Mock private MailSender mailSender;

  private InstitutionalPasswordRecoveryMailService mailService;

  @BeforeEach
  void setUp() {
    final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setCacheable(false);

    final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    mailService =
        new InstitutionalPasswordRecoveryMailService(
            mailSender,
            new MailProperties("no-reply@example.com"),
            new PasswordRecoveryProperties("http://localhost:3000", Duration.ofMinutes(30)),
            templateEngine);
  }

  @Test
  void sendRendersPasswordRecoveryTemplateAndDelegatesToMailSender() {
    final InstitutionalPasswordRecoveryRequested event =
        new InstitutionalPasswordRecoveryRequested(
            UUID.randomUUID(),
            "ana@example.com",
            "Conservatorio Superior de Música Felipe Boero",
            "Ana García",
            "token-123");

    mailService.send(event);

    final ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailSender).send(mailCaptor.capture());

    final MailMessage message = mailCaptor.getValue();
    assertThat(message.from()).isEqualTo("no-reply@example.com");
    assertThat(message.to()).isEqualTo("ana@example.com");
    assertThat(message.subject()).isEqualTo("Recuperación de contraseña");
    assertThat(message.htmlBody())
        .contains("http://localhost:3000/auth/password-recovery/reset?token=token-123")
        .contains("http://localhost:3000/boero-logo.png")
        .contains("Conservatorio Superior de Música Felipe Boero")
        .contains("Hola,")
        .contains("Ana García")
        .contains("30 minutos")
        .doesNotContain("${resetUrl}", "th:href", "th:src", "th:text");
  }
}
