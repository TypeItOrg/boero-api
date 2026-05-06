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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserSessionRepository userSessionRepository;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthResponse execute(RefreshTokenRequest request) {
    String hash = JwtService.hashToken(request.refreshToken());
    RefreshToken current =
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
        userRepository.findById(session.getUserId()).orElseThrow(TokenRefreshException::invalid);

    current.setRevoked(true);
    refreshTokenRepository.save(current);

    String rawRefresh = jwtService.generateRefreshToken();
    RefreshToken next =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(current.getFamilyId())
            .expiresAt(LocalDateTime.now().plus(refreshDuration(session.isRememberMe())))
            .build();
    refreshTokenRepository.save(next);

    String accessToken = jwtService.generateAccessToken(user, session.getId());
    return AuthResponse.of(user, accessToken, rawRefresh);
  }

  private void handleReuse(RefreshToken current) {
    var inFamily = refreshTokenRepository.findByFamilyId(current.getFamilyId());
    Set<UUID> sessionIds = new HashSet<>();
    for (RefreshToken t : inFamily) {
      sessionIds.add(t.getSessionId());
    }
    refreshTokenRepository.revokeByFamilyId(current.getFamilyId());
    LocalDateTime now = LocalDateTime.now();
    for (UUID sessionId : sessionIds) {
      userSessionRepository
          .findById(sessionId)
          .ifPresent(
              s -> {
                s.setActive(false);
                s.setEndedAt(now);
                userSessionRepository.save(s);
              });
    }
    throw TokenRefreshException.reuse();
  }

  private java.time.Duration refreshDuration(boolean rememberMe) {
    return rememberMe
        ? jwtProperties.rememberMeTokenExpiration()
        : jwtProperties.refreshTokenExpiration();
  }
}
