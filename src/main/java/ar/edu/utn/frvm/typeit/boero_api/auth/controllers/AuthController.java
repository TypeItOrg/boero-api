package ar.edu.utn.frvm.typeit.boero_api.auth.controllers;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RegisterRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.ActiveSessionResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserRegisteredResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetActiveSessionsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetCurrentUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LoginUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LogoutUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RefreshTokenUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RegisterUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.common.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.Version;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUseCase loginUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final LogoutUseCase logoutUseCase;
  private final GetActiveSessionsUseCase getActiveSessionsUseCase;
  private final GetCurrentUserUseCase getCurrentUserUseCase;

  @PostMapping(version = Version.V1, path = "/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserRegisteredResponse register(@Valid @RequestBody RegisterRequest request) {
    return registerUserUseCase.execute(request);
  }

  @PostMapping(version = Version.V1, path = "/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return loginUseCase.execute(request, httpRequest);
  }

  @PostMapping(version = Version.V1, path = "/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return refreshTokenUseCase.execute(request);
  }

  @PostMapping(version = Version.V1, path = "/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      Authentication authentication) {
    JwtAuthenticatedUser principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    String token = bearerValue(authorization);
    logoutUseCase.execute(principal, token);
  }

  @GetMapping(version = Version.V1, path = "/sessions")
  public PaginatedResponse<ActiveSessionResponse> sessions(
      Authentication authentication,
      @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    JwtAuthenticatedUser principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    return getActiveSessionsUseCase.execute(principal, pageable);
  }

  @GetMapping(version = Version.V1, path = "/me")
  public UserResponse me(Authentication authentication) {
    JwtAuthenticatedUser principal = (JwtAuthenticatedUser) authentication.getPrincipal();
    return getCurrentUserUseCase.execute(principal);
  }

  private static String bearerValue(String authorization) {
    if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return authorization.substring(7).trim();
    }
    return "";
  }
}
