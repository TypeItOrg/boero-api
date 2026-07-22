package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformRefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PlatformLoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.PlatformUsername;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformLoginUseCase {

  private final AuthenticationManager authenticationManager;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  @Transactional
  public PlatformAuthResponse execute(
      PlatformLoginRequest request, HttpServletRequest httpRequest) {
    String principal = PlatformUsername.format(request.email());

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(principal, request.password()));
    } catch (BadCredentialsException e) {
      throw new InvalidCredentialsException();
    }

    PlatformAccount account = (PlatformAccount) authentication.getPrincipal();
    boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());

    PlatformSession session =
        PlatformSession.builder()
            .platformAccountId(account.getId())
            .ipAddress(AuthRequestMetadata.clientIp(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .rememberMe(rememberMe)
            .build();
    platformSessionRepository.save(session);

    String familyId = UUID.randomUUID().toString();
    String rawRefresh = UUID.randomUUID().toString();
    PlatformRefreshToken refreshToken =
        PlatformRefreshToken.builder()
            .platformSessionId(session.getId())
            .platformAccountId(account.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(familyId)
            .expiresAt(LocalDateTime.now().plus(jwtProperties.refreshExpiration(rememberMe)))
            .build();
    platformRefreshTokenRepository.save(refreshToken);

    String accessToken =
        jwtService.generatePlatformAccessToken(
            PlatformAccessTokenInput.builder()
                .platformAccountId(account.getId())
                .email(account.getEmail())
                .sessionId(session.getId())
                .build());
    log.info("[Auth] Platform login succeeded, platformAccountId: {}", account.getId());

    return PlatformAuthResponse.of(account, accessToken, rawRefresh);
  }
}
