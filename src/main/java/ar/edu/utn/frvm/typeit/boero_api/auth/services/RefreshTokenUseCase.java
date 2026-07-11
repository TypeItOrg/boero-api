package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

  private static final String ACTIVE_SESSIONS_CACHE = "activeSessions";

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserSessionRepository userSessionRepository;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final CacheManager cacheManager;

  @Transactional(noRollbackFor = TokenRefreshException.class)
  public AuthResponse execute(final RefreshTokenRequest request) {
    final String hash = JwtService.hashToken(request.refreshToken());
    final RefreshToken current =
        refreshTokenRepository.findByTokenHash(hash).orElseThrow(TokenRefreshException::invalid);

    if (current.isRevoked()) {
      handleReuse(current);
    }

    if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw TokenRefreshException.invalid();
    }

    UserSession session =
        userSessionRepository
            .findById(current.getSessionId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!session.isActive()) {
      throw TokenRefreshException.invalid();
    }

    User user =
        userRepository
            .findWithPersonAndInstitutionById(session.getUserId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!user.isEnabled()) {
      throw TokenRefreshException.invalid();
    }

    current.setRevoked(true);
    refreshTokenRepository.save(current);

    String rawRefresh = UUID.randomUUID().toString();
    RefreshToken next =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(current.getFamilyId())
            .expiresAt(
                LocalDateTime.now().plus(jwtProperties.refreshExpiration(session.isRememberMe())))
            .build();
    refreshTokenRepository.save(next);

    String accessToken =
        jwtService.generateAccessToken(
            InstitutionalAccessTokenInput.builder()
                .userId(user.getId())
                .personId(user.getPerson().getId())
                .institutionId(user.getInstitutionId())
                .documentNumber(user.getDocumentNumber())
                .sessionId(session.getId())
                .build());
    return AuthResponse.of(user, user.getPerson().getId(), accessToken, rawRefresh);
  }

  private void handleReuse(RefreshToken current) {
    var inFamily = refreshTokenRepository.findByFamilyId(current.getFamilyId());
    Set<UUID> sessionIds = new HashSet<>();
    for (RefreshToken t : inFamily) {
      sessionIds.add(t.getSessionId());
    }
    refreshTokenRepository.revokeByFamilyId(current.getFamilyId());
    LocalDateTime now = LocalDateTime.now();
    if (!sessionIds.isEmpty()) {
      userSessionRepository.deactivateByIds(sessionIds, now);
      final var cache = cacheManager.getCache(ACTIVE_SESSIONS_CACHE);
      if (cache != null) {
        sessionIds.forEach(cache::evict);
      }
    }
    throw TokenRefreshException.reuse();
  }
}
