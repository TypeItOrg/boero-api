package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

public final class InstitutionMessages {

  public static final String INSTITUTION_NOT_FOUND = "La institución especificada no existe.";
  public static final String INSTITUTION_INACTIVE = "La institución especificada no está activa.";
  public static final String SLUG_ALREADY_EXISTS = "Ya existe una institución con ese slug.";
  public static final String CITY_NOT_FOUND = "La ciudad especificada no existe.";
  public static final String PERSON_ALREADY_EXISTS =
      "Ya existe una persona con ese documento en la institución.";

  private InstitutionMessages() {}
}
