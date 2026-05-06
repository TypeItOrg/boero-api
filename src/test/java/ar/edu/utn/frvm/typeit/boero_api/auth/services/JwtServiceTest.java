package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    JwtProperties props =
        new JwtProperties(
            "0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15),
            Duration.ofDays(7),
            Duration.ofDays(30));
    jwtService = new JwtService(props);
    jwtService.init();
  }

  @Test
  void generateAndParseAccessToken_includesJtiAndSessionId() {
    Institution institution = Institution.builder().id(UUID.randomUUID()).build();
    Person person = Person.builder().documentNumber("12345678").build();
    User user = User.builder().id(UUID.randomUUID()).institution(institution).person(person).build();
    UUID sessionId = UUID.randomUUID();

    String token = jwtService.generateAccessToken(user, sessionId);

    assertThat(jwtService.parseAccessToken(token))
        .isInstanceOf(JwtService.AccessTokenParseResult.Ok.class);

    var ok = (JwtService.AccessTokenParseResult.Ok) jwtService.parseAccessToken(token);
    assertThat(jwtService.extractJti(ok.claims())).isNotBlank();
    assertThat(jwtService.extractUserId(ok.claims())).isEqualTo(user.getId());
    assertThat(jwtService.extractSessionId(ok.claims())).isEqualTo(sessionId);
    assertThat(jwtService.extractDocumentNumber(ok.claims())).isEqualTo("12345678");
    assertThat(jwtService.extractInstitutionId(ok.claims())).isEqualTo(institution.getId());
  }

  @Test
  void hashToken_isDeterministic() {
    String raw = UUID.randomUUID().toString();
    assertThat(JwtService.hashToken(raw)).isEqualTo(JwtService.hashToken(raw));
  }
}
