package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
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
public class LoginUseCase {

  private final CredentialsAuthenticator credentialsAuthenticator;
  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final AuthorityResolver authorityResolver;
  private final RefreshTokenGenerator refreshTokenGenerator;

  @Transactional
  public AuthResponse execute(final LoginRequest request, final HttpServletRequest httpRequest) {
    final String principal =
        InstitutionalUsername.format(request.institutionId(), request.documentNumber());

    final Authentication authentication =
        credentialsAuthenticator.authenticate(principal, request.password());

    final User user = (User) authentication.getPrincipal();

    final boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());

    final UserSession session =
        userSessionRepository.save(
            UserSession.builder()
                .userId(user.getId())
                .ipAddress(AuthRequestMetadata.clientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .rememberMe(rememberMe)
                .build());

    final String familyId = refreshTokenGenerator.newFamilyId();
    final GeneratedRefreshToken generatedRefreshToken = refreshTokenGenerator.generate();
    final RefreshToken refreshToken =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash(generatedRefreshToken.tokenHash())
            .familyId(familyId)
            .expiresAt(LocalDateTime.now().plus(jwtProperties.refreshExpiration(rememberMe)))
            .build();
    refreshTokenRepository.save(refreshToken);

    final String accessToken =
        jwtService.generateAccessToken(
            InstitutionalAccessTokenInput.builder()
                .userId(user.getId())
                .personId(user.getPerson().getId())
                .institutionId(user.getInstitutionId())
                .documentNumber(user.getDocumentNumber())
                .sessionId(session.getId())
                .build());
    log.info(
        "[Auth] Login succeeded, userId: {}, institutionId: {}",
        user.getId(),
        user.getInstitutionId());

    return AuthResponse.of(
        user,
        user.getPerson().getId(),
        authorityResolver.resolveForPerson(user.getPerson().getId(), user.getInstitutionId()),
        accessToken,
        generatedRefreshToken.rawToken());
  }
}
