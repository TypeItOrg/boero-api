package ar.edu.utn.frvm.typeit.boero_api.authorization.cache;

import java.util.UUID;

public final class AuthorizationCacheKeys {

  private AuthorizationCacheKeys() {}

  public static String person(UUID personId, UUID institutionId) {
    return personId + "-" + institutionId;
  }

  public static String platformAccount(UUID platformAccountId) {
    return platformAccountId.toString();
  }
}
