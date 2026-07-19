package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

public final class AuthorizationMessages {

  public static final String PERSON_NOT_FOUND_IN_INSTITUTION =
      "La persona especificada no pertenece a la institución indicada.";
  public static final String ROLE_NOT_ASSIGNABLE =
      "El rol especificado no puede asignarse a una persona institucional.";
  public static final String LAST_PERSON_ROLE_REVOCATION =
      "No se puede revocar el único rol del usuario. Asigne otro rol antes.";

  private AuthorizationMessages() {}
}
