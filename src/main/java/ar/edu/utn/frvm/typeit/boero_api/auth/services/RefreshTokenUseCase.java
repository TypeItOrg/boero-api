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
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
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
  private final AuthorityResolver authorityResolver;
  private final RefreshReplayCache replayCache;

  @Transactional(noRollbackFor = TokenRefreshException.class)
  public AuthResponse execute(final RefreshTokenRequest request) {
    final String hash = JwtService.hashToken(request.refreshToken());
    final RefreshToken current =
        refreshTokenRepository.findByTokenHash(hash).orElseThrow(TokenRefreshException::invalid);

    if (current.isRevoked()) {
      final Optional<RefreshReplay> replay = replayCache.getInstitutional(hash);
      if (replay.isPresent()) {
        final UserSession session = findActiveSession(current);
        final User user = findEnabledUser(session);
        return createResponse(
            user, session, replay.get().accessToken(), replay.get().refreshToken());
      }
      handleReuse(current);
    }

    if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw TokenRefreshException.invalid();
    }

    final UserSession session = findActiveSession(current);
    final User user = findEnabledUser(session);

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
    final AuthResponse response = createResponse(user, session, accessToken, rawRefresh);
    replayCache.putInstitutional(hash, new RefreshReplay(accessToken, rawRefresh));
    return response;
  }

  private UserSession findActiveSession(final RefreshToken current) {
    final UserSession session =
        userSessionRepository
            .findById(current.getSessionId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!session.isActive()) {
      throw TokenRefreshException.invalid();
    }

    return session;
  }

  private User findEnabledUser(final UserSession session) {
    final User user =
        userRepository
            .findWithPersonAndInstitutionById(session.getUserId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!user.isEnabled()) {
      throw TokenRefreshException.invalid();
    }

    return user;
  }

  private AuthResponse createResponse(
      final User user,
      final UserSession session,
      final String accessToken,
      final String refreshToken) {
    return AuthResponse.of(
        user,
        user.getPerson().getId(),
        authorityResolver.resolveForPerson(user.getPerson().getId(), user.getInstitutionId()),
        accessToken,
        refreshToken);
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
