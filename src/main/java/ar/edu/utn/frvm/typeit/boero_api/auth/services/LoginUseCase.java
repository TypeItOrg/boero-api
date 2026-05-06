package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.UserDisabledException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
    User user =
        userRepository
            .findByPersonDocumentNumberAndInstitution_Id(
                request.documentNumber(), request.institutionId())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new InvalidCredentialsException();
    }

    if (!user.isEnabled()) {
      throw new UserDisabledException();
    }

    boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());

    UserSession session =
        UserSession.builder()
            .userId(user.getId())
            .ipAddress(clientIp(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .rememberMe(rememberMe)
            .build();
    userSessionRepository.save(session);

    String familyId = UUID.randomUUID().toString();
    String rawRefresh = jwtService.generateRefreshToken();
    RefreshToken refreshToken =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(familyId)
            .expiresAt(LocalDateTime.now().plus(refreshDuration(rememberMe)))
            .build();
    refreshTokenRepository.save(refreshToken);

    String accessToken = jwtService.generateAccessToken(user, session.getId());
    return AuthResponse.of(user, accessToken, rawRefresh);
  }

  private Duration refreshDuration(boolean rememberMe) {
    return rememberMe
        ? jwtProperties.rememberMeTokenExpiration()
        : jwtProperties.refreshTokenExpiration();
  }

  private static String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
