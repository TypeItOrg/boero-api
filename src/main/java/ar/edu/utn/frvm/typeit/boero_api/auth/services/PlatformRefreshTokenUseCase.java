package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformRefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformSessionRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformRefreshTokenUseCase {

  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  @Transactional
  public PlatformAuthResponse execute(RefreshTokenRequest request) {
    String hash = JwtService.hashToken(request.refreshToken());
    PlatformRefreshToken current =
        platformRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(TokenRefreshException::invalid);

    if (current.isRevoked()) {
      handleReuse(current);
    }

    if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw TokenRefreshException.invalid();
    }

    PlatformSession session =
        platformSessionRepository
            .findById(current.getPlatformSessionId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!session.isActive()) {
      throw TokenRefreshException.invalid();
    }

    PlatformAccount account =
        platformAccountRepository
            .findById(current.getPlatformAccountId())
            .orElseThrow(TokenRefreshException::invalid);

    current.setRevoked(true);
    platformRefreshTokenRepository.save(current);

    String rawRefresh = UUID.randomUUID().toString();
    PlatformRefreshToken next =
        PlatformRefreshToken.builder()
            .platformSessionId(session.getId())
            .platformAccountId(account.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(current.getFamilyId())
            .expiresAt(
                LocalDateTime.now().plus(jwtProperties.refreshExpiration(session.isRememberMe())))
            .build();
    platformRefreshTokenRepository.save(next);

    String accessToken =
        jwtService.generatePlatformAccessToken(
            PlatformAccessTokenInput.builder()
                .platformAccountId(account.getId())
                .email(account.getEmail())
                .sessionId(session.getId())
                .build());
    return PlatformAuthResponse.of(account, accessToken, rawRefresh);
  }

  private void handleReuse(PlatformRefreshToken current) {
    var inFamily = platformRefreshTokenRepository.findByFamilyId(current.getFamilyId());
    Set<UUID> sessionIds = new HashSet<>();
    for (PlatformRefreshToken token : inFamily) {
      sessionIds.add(token.getPlatformSessionId());
    }
    platformRefreshTokenRepository.revokeByFamilyId(current.getFamilyId());
    LocalDateTime now = LocalDateTime.now();
    if (!sessionIds.isEmpty()) {
      platformSessionRepository.deactivateByIds(sessionIds, now);
    }
    throw TokenRefreshException.reuse();
  }
}
