package ar.edu.utn.frvm.typeit.boero_api.authorization.security;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import org.springframework.security.access.AccessDeniedException;

final class AuthorizationAspectSupport {

  private AuthorizationAspectSupport() {}

  static void denyUnless(boolean authorized) {
    if (!authorized) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }
  }
}
