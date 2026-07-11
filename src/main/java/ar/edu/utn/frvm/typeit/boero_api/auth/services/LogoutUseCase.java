package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.AccessTokenParseResult;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import java.time.LocalDateTime;
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

  @org.springframework.cache.annotation.CacheEvict(
      value = "activeSessions",
      key = "#principal.sessionId")
  @Transactional
  public void execute(JwtAuthenticatedUser principal, String accessToken) {
    switch (jwtService.parseAccessToken(accessToken)) {
      case AccessTokenParseResult.Ok(var claims) -> {
        String tokenId = jwtService.extractTokenId(claims);
        tokenBlacklistService.blacklist(
            tokenId, TokenBlacklistTtl.remaining(claims.getExpiration().toInstant()));
      }
      case AccessTokenParseResult.Expired() -> {}
      case AccessTokenParseResult.Invalid() -> {}
    }

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
