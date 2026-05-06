package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtProperties jwtProperties;
  private SecretKey secretKey;

  public sealed interface AccessTokenParseResult
      permits AccessTokenParseResult.Ok,
          AccessTokenParseResult.Expired,
          AccessTokenParseResult.Invalid {
    record Ok(Claims claims) implements AccessTokenParseResult {}

    record Expired() implements AccessTokenParseResult {}

    record Invalid() implements AccessTokenParseResult {}
  }

  @PostConstruct
  void init() {
    this.secretKey =
        Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(User user, UUID sessionId) {
    Instant now = Instant.now();
    Instant expiration = now.plus(jwtProperties.accessTokenExpiration());
    UUID jti = UUID.randomUUID();

    return Jwts.builder()
        .id(jti.toString())
        .subject(user.getId().toString())
        .claim("documentNumber", user.getDocumentNumber())
        .claim("institutionId", user.getInstitutionId().toString())
        .claim("sessionId", sessionId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken() {
    return UUID.randomUUID().toString();
  }

  public AccessTokenParseResult parseAccessToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
      return new AccessTokenParseResult.Ok(claims);
    } catch (ExpiredJwtException e) {
      return new AccessTokenParseResult.Expired();
    } catch (JwtException | IllegalArgumentException e) {
      return new AccessTokenParseResult.Invalid();
    }
  }

  public Optional<Claims> parseAndValidate(String token) {
    return switch (parseAccessToken(token)) {
      case AccessTokenParseResult.Ok(var c) -> Optional.of(c);
      case AccessTokenParseResult.Expired() -> Optional.empty();
      case AccessTokenParseResult.Invalid() -> Optional.empty();
    };
  }

  public UUID extractUserId(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }

  public String extractJti(Claims claims) {
    return claims.getId();
  }

  public String extractDocumentNumber(Claims claims) {
    return claims.get("documentNumber", String.class);
  }

  public UUID extractInstitutionId(Claims claims) {
    return UUID.fromString(claims.get("institutionId", String.class));
  }

  public UUID extractSessionId(Claims claims) {
    return UUID.fromString(claims.get("sessionId", String.class));
  }

  public long getAccessTokenExpirationSeconds() {
    return jwtProperties.accessTokenExpiration().toSeconds();
  }

  public static String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
