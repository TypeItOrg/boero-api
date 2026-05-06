package ar.edu.utn.frvm.typeit.boero_api.security;

public final class SecurityConstants {

  public static final String[] AUTH_ROUTES = {
    "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh"
  };

  public static final String[] PUBLIC_ROUTES = {"/actuator/health/**", "/api/v1/greeting/**"};
}
