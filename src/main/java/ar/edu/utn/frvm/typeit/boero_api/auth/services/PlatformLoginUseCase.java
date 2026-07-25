package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformRefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PlatformLoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.PlatformUsername;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformLoginUseCase {

  private final CredentialsAuthenticator credentialsAuthenticator;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final RefreshTokenGenerator refreshTokenGenerator;

  @Transactional
  public PlatformAuthResponse execute(
      final PlatformLoginRequest request, final HttpServletRequest httpRequest) {
    final String principal = PlatformUsername.format(request.email());

    final Authentication authentication =
        credentialsAuthenticator.authenticate(principal, request.password());

    final PlatformAccount account = (PlatformAccount) authentication.getPrincipal();
    final boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());

    final PlatformSession session =
        platformSessionRepository.save(
            PlatformSession.builder()
                .platformAccountId(account.getId())
                .ipAddress(AuthRequestMetadata.clientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .rememberMe(rememberMe)
                .build());

    final String familyId = refreshTokenGenerator.newFamilyId();
    final GeneratedRefreshToken generatedRefreshToken = refreshTokenGenerator.generate();
    final PlatformRefreshToken refreshToken =
        PlatformRefreshToken.builder()
            .platformSessionId(session.getId())
            .platformAccountId(account.getId())
            .tokenHash(generatedRefreshToken.tokenHash())
            .familyId(familyId)
            .expiresAt(LocalDateTime.now().plus(jwtProperties.refreshExpiration(rememberMe)))
            .build();
    platformRefreshTokenRepository.save(refreshToken);

    final String accessToken =
        jwtService.generatePlatformAccessToken(
            PlatformAccessTokenInput.builder()
                .platformAccountId(account.getId())
                .email(account.getEmail())
                .sessionId(session.getId())
                .build());
    log.info("[Auth] Platform login succeeded, platformAccountId: {}", account.getId());

    return PlatformAuthResponse.of(account, accessToken, generatedRefreshToken.rawToken());
  }
}
