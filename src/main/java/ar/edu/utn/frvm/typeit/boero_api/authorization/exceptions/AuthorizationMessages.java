package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

public final class AuthorizationMessages {

  public static final String PERSON_NOT_FOUND_IN_INSTITUTION =
      "La persona especificada no pertenece a la institución indicada.";
  public static final String ROLE_NOT_ASSIGNABLE =
      "El rol especificado no puede asignarse a una persona institucional.";
  public static final String LAST_INSTITUTIONAL_AUTHORITY_REVOCATION =
      "No se puede revocar la última autoridad institucional de la institución.";
  public static final String LAST_INSTITUTIONAL_AUTHORITY_DELETION =
      "No se puede eliminar a la última autoridad institucional de la institución. Asigne otra autoridad antes.";

  private AuthorizationMessages() {}
}
