package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

  private final AuthenticationManager authenticationManager;
  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final AuthorityResolver authorityResolver;

  @Transactional
  public AuthResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
    String principal =
        InstitutionalUsername.format(request.institutionId(), request.documentNumber());

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(principal, request.password()));
    } catch (BadCredentialsException e) {
      throw new InvalidCredentialsException();
    }

    User user = (User) authentication.getPrincipal();

    boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());

    UserSession session =
        UserSession.builder()
            .userId(user.getId())
            .ipAddress(AuthRequestMetadata.clientIp(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .rememberMe(rememberMe)
            .build();
    userSessionRepository.save(session);

    String familyId = UUID.randomUUID().toString();
    String rawRefresh = UUID.randomUUID().toString();
    RefreshToken refreshToken =
        RefreshToken.builder()
            .sessionId(session.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(familyId)
            .expiresAt(LocalDateTime.now().plus(jwtProperties.refreshExpiration(rememberMe)))
            .build();
    refreshTokenRepository.save(refreshToken);

    String accessToken =
        jwtService.generateAccessToken(
            InstitutionalAccessTokenInput.builder()
                .userId(user.getId())
                .personId(user.getPerson().getId())
                .institutionId(user.getInstitutionId())
                .documentNumber(user.getDocumentNumber())
                .sessionId(session.getId())
                .build());
    return AuthResponse.of(
        user,
        user.getPerson().getId(),
        authorityResolver.resolveForPerson(user.getPerson().getId(), user.getInstitutionId()),
        accessToken,
        rawRefresh);
  }
}
