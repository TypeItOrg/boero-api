package ar.edu.utn.frvm.typeit.boero_api.auth.filters;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final TokenBlacklistService tokenBlacklistService;
  private final UserSessionRepository userSessionRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.MISSING_TOKEN);
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.INVALID_TOKEN);
      filterChain.doFilter(request, response);
      return;
    }

    switch (jwtService.parseAccessToken(token)) {
      case JwtService.AccessTokenParseResult.Expired() -> {
        request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.EXPIRED_TOKEN);
        filterChain.doFilter(request, response);
        return;
      }
      case JwtService.AccessTokenParseResult.Invalid() -> {
        request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.INVALID_TOKEN);
        filterChain.doFilter(request, response);
        return;
      }
      case JwtService.AccessTokenParseResult.Ok(var claims) -> {
        String jti = jwtService.extractJti(claims);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
          request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.BLACKLISTED_TOKEN);
          filterChain.doFilter(request, response);
          return;
        }
        if (!isSessionActive(claims)) {
          request.setAttribute(AuthRequestAttributes.AUTH_ERROR, AuthError.INACTIVE_SESSION);
          filterChain.doFilter(request, response);
          return;
        }
        setSecurityContext(claims, jti);
        filterChain.doFilter(request, response);
      }
    }
  }

  private void setSecurityContext(Claims claims, String jti) {
    UUID userId = jwtService.extractUserId(claims);
    UUID sessionId = jwtService.extractSessionId(claims);
    JwtAuthenticatedUser principal =
        new JwtAuthenticatedUser(
            userId,
            jwtService.extractDocumentNumber(claims),
            jwtService.extractInstitutionId(claims),
            sessionId,
            jti != null ? jti : "");

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private boolean isSessionActive(Claims claims) {
    UUID sessionId = jwtService.extractSessionId(claims);
    return userSessionRepository
        .findById(sessionId)
        .filter(session -> session.isActive())
        .isPresent();
  }
}
