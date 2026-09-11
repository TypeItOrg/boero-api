package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationSessionIssuer {

  public static final String METHOD_PASSWORD = "PASSWORD";
  public static final String METHOD_PASSKEY = "PASSKEY";

  private final LoginSessionPersistenceService loginSessionPersistenceService;
  private final JwtService jwtService;
  private final AuthorityResolver authorityResolver;
  private final RecentAuthService recentAuthService;

  public AuthResponse issue(
      final User user,
      final String ipAddress,
      final String userAgent,
      final boolean rememberMe,
      final String method) {
    final var authorities =
        authorityResolver.resolveForPerson(user.getPerson().getId(), user.getInstitutionId());
    final LoginSessionPersistenceService.Result session =
        loginSessionPersistenceService.create(user.getId(), ipAddress, userAgent, rememberMe);
    final String accessToken =
        jwtService.generateAccessToken(
            InstitutionalAccessTokenInput.builder()
                .userId(user.getId())
                .personId(user.getPerson().getId())
                .institutionId(user.getInstitutionId())
                .documentNumber(user.getDocumentNumber())
                .sessionId(session.sessionId())
                .build());
    recentAuthService.mark(session.sessionId(), user.getId(), method);
    log.info(
        "[Auth] Login succeeded, userId: {}, institutionId: {}",
        user.getId(),
        user.getInstitutionId());
    return AuthResponse.of(
        user, user.getPerson().getId(), authorities, accessToken, session.refreshToken());
  }
}
