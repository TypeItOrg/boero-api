package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserSessionRepository userSessionRepository;
  private final JwtService jwtService;

  @Transactional
  public void execute(JwtAuthenticatedUser principal, String accessToken) {
    Optional<Claims> claimsOpt = jwtService.parseAndValidate(accessToken);
    claimsOpt.ifPresent(
        claims -> {
          String jti = jwtService.extractJti(claims);
          if (jti != null && !jti.isEmpty()) {
            Instant exp = claims.getExpiration().toInstant();
            Duration ttl = Duration.between(Instant.now(), exp);
            if (ttl.isNegative() || ttl.isZero()) {
              ttl = Duration.ofMinutes(1);
            }
            tokenBlacklistService.blacklist(jti, ttl);
          }
        });

    refreshTokenRepository.revokeBySessionId(principal.sessionId());
    userSessionRepository
        .findById(principal.sessionId())
        .ifPresent(
            s -> {
              s.setActive(false);
              s.setEndedAt(LocalDateTime.now());
              userSessionRepository.save(s);
            });
  }
}
