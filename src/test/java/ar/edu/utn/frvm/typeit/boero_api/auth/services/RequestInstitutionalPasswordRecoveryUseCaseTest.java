package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.config.PasswordRecoveryProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.events.InstitutionalPasswordRecoveryRequested;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitExceededException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.InstitutionalPasswordResetTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PasswordRecoveryRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.Clock;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RequestInstitutionalPasswordRecoveryUseCaseTest {

  private static final String DOCUMENT_NUMBER = "12345678";
  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(30);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC);

  @Mock private UserRepository userRepository;

  @Mock private InstitutionalPasswordResetTokenRepository passwordResetTokenRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private AuthRateLimitService rateLimitService;

  @Mock private AuthRateLimitProperties rateLimitProperties;

  @Mock private HttpServletRequest httpRequest;

  private RequestInstitutionalPasswordRecoveryUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new RequestInstitutionalPasswordRecoveryUseCase(
            CLOCK,
            userRepository,
            passwordResetTokenRepository,
            eventPublisher,
            new PasswordRecoveryProperties("http://localhost:3000", TOKEN_EXPIRATION),
            rateLimitService,
            rateLimitProperties);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  @Test
  @DisplayName("publica el evento con los datos resueltos cuando el usuario puede recuperar")
  void publishesResolvedEventWhenUserCanRecover() {
    // given
    final User user = recoverableUser();
    when(userRepository.findWithPersonAndInstitutionForPasswordRecovery(
            DOCUMENT_NUMBER, INSTITUTION_ID))
        .thenReturn(Optional.of(user));

    // when
    useCase.execute(new PasswordRecoveryRequest(DOCUMENT_NUMBER, INSTITUTION_ID), httpRequest);

    // then
    verify(passwordResetTokenRepository).deleteByUserId(USER_ID);

    final ArgumentCaptor<InstitutionalPasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(InstitutionalPasswordResetToken.class);
    verify(passwordResetTokenRepository).save(tokenCaptor.capture());

    final ArgumentCaptor<InstitutionalPasswordRecoveryRequested> eventCaptor =
        ArgumentCaptor.forClass(InstitutionalPasswordRecoveryRequested.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    final InstitutionalPasswordRecoveryRequested event = eventCaptor.getValue();
    assertThat(event.userId()).isEqualTo(USER_ID);
    assertThat(event.recipientEmail()).isEqualTo("ana@example.com");
    assertThat(event.institutionName()).isEqualTo("Conservatorio Superior de Música Felipe Boero");
    assertThat(event.fullName()).isEqualTo("Ana García");
    assertThat(event.token()).isNotBlank();

    assertThat(tokenCaptor.getValue().getTokenHash())
        .isEqualTo(RequestInstitutionalPasswordRecoveryUseCase.hash(event.token()));
    assertThat(tokenCaptor.getValue().getExpiresAt())
        .isEqualTo(CLOCK.instant().plus(TOKEN_EXPIRATION));
  }

  @Test
  @DisplayName("no persiste ni publica nada cuando el usuario no existe")
  void publishesNothingWhenUserNotFound() {
    when(userRepository.findWithPersonAndInstitutionForPasswordRecovery(
            DOCUMENT_NUMBER, INSTITUTION_ID))
        .thenReturn(Optional.empty());

    useCase.execute(new PasswordRecoveryRequest(DOCUMENT_NUMBER, INSTITUTION_ID), httpRequest);

    verifyNoInteractions(passwordResetTokenRepository, eventPublisher);
  }

  @Test
  @DisplayName("no publica el evento cuando el acceso del usuario está deshabilitado")
  void publishesNothingWhenUserAccessDisabled() {
    final User user =
        User.builder()
            .id(USER_ID)
            .institution(activeInstitution())
            .person(personWithEmail())
            .enabled(false)
            .build();
    when(userRepository.findWithPersonAndInstitutionForPasswordRecovery(
            DOCUMENT_NUMBER, INSTITUTION_ID))
        .thenReturn(Optional.of(user));

    useCase.execute(new PasswordRecoveryRequest(DOCUMENT_NUMBER, INSTITUTION_ID), httpRequest);

    verifyNoInteractions(passwordResetTokenRepository, eventPublisher);
  }

  @Test
  @DisplayName("no publica el evento cuando la persona no tiene email")
  void publishesNothingWhenPersonHasNoEmail() {
    final Person personWithoutEmail = Person.builder().firstName("Ana").lastName("García").build();
    final User user =
        User.builder()
            .id(USER_ID)
            .institution(activeInstitution())
            .person(personWithoutEmail)
            .build();
    when(userRepository.findWithPersonAndInstitutionForPasswordRecovery(
            DOCUMENT_NUMBER, INSTITUTION_ID))
        .thenReturn(Optional.of(user));

    useCase.execute(new PasswordRecoveryRequest(DOCUMENT_NUMBER, INSTITUTION_ID), httpRequest);

    verifyNoInteractions(passwordResetTokenRepository, eventPublisher);
  }

  @Test
  @DisplayName("propaga el límite de intentos sin revelar si la cuenta existe")
  void propagatesRateLimitWithoutRevealingAccountExistence() {
    doThrow(new RateLimitExceededException())
        .when(rateLimitService)
        .checkAllowed(anyString(), any(), anyInt(), any());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new PasswordRecoveryRequest(DOCUMENT_NUMBER, INSTITUTION_ID), httpRequest))
        .isInstanceOf(RateLimitExceededException.class);

    verifyNoInteractions(userRepository, passwordResetTokenRepository, eventPublisher);
  }

  private User recoverableUser() {
    return User.builder()
        .id(USER_ID)
        .institution(activeInstitution())
        .person(personWithEmail())
        .build();
  }

  private Institution activeInstitution() {
    return Institution.builder().name("Conservatorio Superior de Música Felipe Boero").build();
  }

  private Person personWithEmail() {
    return Person.builder().firstName("Ana").lastName("García").email("ana@example.com").build();
  }
}
