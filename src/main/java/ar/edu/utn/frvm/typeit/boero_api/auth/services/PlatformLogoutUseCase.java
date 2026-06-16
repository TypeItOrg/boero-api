package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.AccessTokenParseResult;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformSessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformLogoutUseCase {

  private final TokenBlacklistService tokenBlacklistService;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;
  private final JwtService jwtService;

  @Transactional
  public void execute(JwtAuthenticatedPlatformAccount principal, String accessToken) {
    switch (jwtService.parseAccessToken(accessToken)) {
      case AccessTokenParseResult.Ok(var claims) -> {
        String tokenId = jwtService.extractTokenId(claims);
        tokenBlacklistService.blacklist(
            tokenId, TokenBlacklistTtl.remaining(claims.getExpiration().toInstant()));
      }
      case AccessTokenParseResult.Expired() -> {}
      case AccessTokenParseResult.Invalid() -> {}
    }

    platformRefreshTokenRepository.revokeByPlatformSessionId(principal.sessionId());
    platformSessionRepository
        .findById(principal.sessionId())
        .ifPresent(
            session -> {
              session.setActive(false);
              session.setEndedAt(LocalDateTime.now());
              platformSessionRepository.save(session);
            });
  }
}
