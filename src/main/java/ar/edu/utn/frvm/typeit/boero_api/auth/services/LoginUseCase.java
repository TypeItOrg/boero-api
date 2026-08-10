package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

  private final CredentialsAuthenticator credentialsAuthenticator;
  private final LoginSessionPersistenceService loginSessionPersistenceService;
  private final JwtService jwtService;
  private final AuthorityResolver authorityResolver;

  public AuthResponse execute(final LoginRequest request, final HttpServletRequest httpRequest) {
    final String principal =
        InstitutionalUsername.format(request.institutionId(), request.documentNumber());

    final Authentication authentication =
        credentialsAuthenticator.authenticate(principal, request.password());

    final User user = (User) authentication.getPrincipal();

    final var authorities =
        authorityResolver.resolveForPerson(user.getPerson().getId(), user.getInstitutionId());
    final boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
    final LoginSessionPersistenceService.Result session =
        loginSessionPersistenceService.create(
            user.getId(),
            AuthRequestMetadata.clientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            rememberMe);

    final String accessToken =
        jwtService.generateAccessToken(
            InstitutionalAccessTokenInput.builder()
                .userId(user.getId())
                .personId(user.getPerson().getId())
                .institutionId(user.getInstitutionId())
                .documentNumber(user.getDocumentNumber())
                .sessionId(session.sessionId())
                .build());
    log.info(
        "[Auth] Login succeeded, userId: {}, institutionId: {}",
        user.getId(),
        user.getInstitutionId());

    return AuthResponse.of(
        user, user.getPerson().getId(), authorities, accessToken, session.refreshToken());
  }
}
