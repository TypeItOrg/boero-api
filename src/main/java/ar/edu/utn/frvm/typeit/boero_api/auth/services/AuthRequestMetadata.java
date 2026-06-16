package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import jakarta.servlet.http.HttpServletRequest;

final class AuthRequestMetadata {

  private AuthRequestMetadata() {}

  static String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
