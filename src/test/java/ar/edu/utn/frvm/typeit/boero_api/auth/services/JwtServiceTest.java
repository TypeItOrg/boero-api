package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.jwtProperties;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.AccessTokenParseResult;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.AccountType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(jwtProperties());
  }

  @Test
  @DisplayName("Should generate a parseable token and extract all claims correctly")
  void generateAndParseAccessToken_extractsAllClaims() {
    UUID userId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    InstitutionalAccessTokenInput input =
        InstitutionalAccessTokenInput.builder()
            .userId(userId)
            .personId(personId)
            .institutionId(institutionId)
            .documentNumber("12345678")
            .sessionId(sessionId)
            .build();

    String token = jwtService.generateAccessToken(input);
    AccessTokenParseResult result = jwtService.parseAccessToken(token);
    assertThat(result).isInstanceOf(AccessTokenParseResult.Ok.class);
    var ok = (AccessTokenParseResult.Ok) result;
    assertThat(jwtService.extractTokenId(ok.claims())).isEqualTo(ok.claims().getId());
    assertThat(jwtService.extractUserId(ok.claims())).isEqualTo(userId);
    assertThat(jwtService.extractPersonId(ok.claims())).isEqualTo(personId);
    assertThat(jwtService.extractSessionId(ok.claims())).isEqualTo(sessionId);
    assertThat(jwtService.extractDocumentNumber(ok.claims())).isEqualTo("12345678");
    assertThat(jwtService.extractInstitutionId(ok.claims())).isEqualTo(institutionId);
    assertThat(jwtService.extractAccountType(ok.claims())).isEqualTo(AccountType.INSTITUTION);
    assertThat(ok.claims().get("authorities")).isNull();
  }

  @Test
  @DisplayName("Should return different token IDs for different generated tokens")
  void generateAccessToken_producesDifferentTokenIds() {
    InstitutionalAccessTokenInput input = testInput();

    String token1 = jwtService.generateAccessToken(input);
    String token2 = jwtService.generateAccessToken(input);

    var ok1 = (AccessTokenParseResult.Ok) jwtService.parseAccessToken(token1);
    var ok2 = (AccessTokenParseResult.Ok) jwtService.parseAccessToken(token2);

    assertThat(jwtService.extractTokenId(ok1.claims()))
        .isNotEqualTo(jwtService.extractTokenId(ok2.claims()));
  }

  @Test
  @DisplayName("Should return Expired when token is already expired")
  void parseAccessToken_returnsExpired_whenTokenIsExpired() {
    JwtProperties expiredProps =
        new JwtProperties(
            jwtProperties().secret(), Duration.ofNanos(1), Duration.ofDays(7), Duration.ofDays(30));
    JwtService shortLivedService = new JwtService(expiredProps);

    String token = shortLivedService.generateAccessToken(testInput());

    assertThat(shortLivedService.parseAccessToken(token))
        .isInstanceOf(AccessTokenParseResult.Expired.class);
  }

  @Test
  @DisplayName("Should return Invalid when token is a random string")
  void parseAccessToken_returnsInvalid_whenTokenIsMalformed() {
    assertThat(jwtService.parseAccessToken("not.a.jwt"))
        .isInstanceOf(AccessTokenParseResult.Invalid.class);
  }

  @Test
  @DisplayName("Should return Invalid when token was signed with a different key")
  void parseAccessToken_returnsInvalid_whenSignedWithDifferentKey() {
    String otherSecret = "ffffffffffffffffffffffffffffffff";
    var otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
    String forgedToken =
        Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .signWith(otherKey)
            .compact();

    assertThat(jwtService.parseAccessToken(forgedToken))
        .isInstanceOf(AccessTokenParseResult.Invalid.class);
  }

  @Test
  @DisplayName("Should produce the same hash for the same input")
  void hashToken_isDeterministic() {
    String raw = UUID.randomUUID().toString();
    assertThat(JwtService.hashToken(raw)).isEqualTo(JwtService.hashToken(raw));
  }

  @Test
  @DisplayName("Should produce different hashes for different inputs")
  void hashToken_producesDifferentHashesForDifferentInputs() {
    assertThat(JwtService.hashToken("token-a")).isNotEqualTo(JwtService.hashToken("token-b"));
  }

  @Test
  @DisplayName("Should generate a parseable platform token without institutional claims")
  void generatePlatformAccessToken_extractsPlatformClaims() {
    UUID platformAccountId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    PlatformAccessTokenInput input =
        PlatformAccessTokenInput.builder()
            .platformAccountId(platformAccountId)
            .email("admin@plataforma.com")
            .sessionId(sessionId)
            .build();

    String token = jwtService.generatePlatformAccessToken(input);
    AccessTokenParseResult result = jwtService.parseAccessToken(token);
    assertThat(result).isInstanceOf(AccessTokenParseResult.Ok.class);
    var ok = (AccessTokenParseResult.Ok) result;
    assertThat(jwtService.extractAccountType(ok.claims())).isEqualTo(AccountType.PLATFORM);
    assertThat(jwtService.extractPlatformAccountId(ok.claims())).isEqualTo(platformAccountId);
    assertThat(jwtService.extractEmail(ok.claims())).isEqualTo("admin@plataforma.com");
    assertThat(jwtService.extractSessionId(ok.claims())).isEqualTo(sessionId);
    assertThat(ok.claims().get("authorities")).isNull();
    assertThat(ok.claims().get("institutionId")).isNull();
  }

  private static InstitutionalAccessTokenInput testInput() {
    return InstitutionalAccessTokenInput.builder()
        .userId(UUID.randomUUID())
        .personId(UUID.randomUUID())
        .institutionId(UUID.randomUUID())
        .documentNumber("12345678")
        .sessionId(UUID.randomUUID())
        .build();
  }
}
