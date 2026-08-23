package ar.edu.utn.frvm.typeit.boero_api.auth.listeners;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frvm.typeit.boero_api.auth.events.InstitutionalPasswordRecoveryRequested;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.InstitutionalPasswordRecoveryMailService;
import ar.edu.utn.frvm.typeit.boero_api.common.mail.MailSendingException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionalPasswordRecoveryMailListenerTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private InstitutionalPasswordRecoveryMailService mailService;

  private InstitutionalPasswordRecoveryMailListener listener;

  @BeforeEach
  void setUp() {
    listener = new InstitutionalPasswordRecoveryMailListener(mailService);
  }

  @Test
  @DisplayName("delega el envío del mail al servicio de mail")
  void delegatesEmailSendingToMailService() {
    final InstitutionalPasswordRecoveryRequested event = recoveryEvent();

    listener.on(event);

    verify(mailService).send(event);
  }

  @Test
  @DisplayName("traga la excepción de envío sin propagarla después del commit")
  void swallowsMailSendingException() {
    final InstitutionalPasswordRecoveryRequested event = recoveryEvent();
    doThrow(new MailSendingException(new RuntimeException("smtp down")))
        .when(mailService)
        .send(event);

    assertThatCode(() -> listener.on(event)).doesNotThrowAnyException();
  }

  private InstitutionalPasswordRecoveryRequested recoveryEvent() {
    return new InstitutionalPasswordRecoveryRequested(
        USER_ID,
        "ana@example.com",
        "Conservatorio Superior de Música Felipe Boero",
        "Ana García",
        "token-123");
  }
}
