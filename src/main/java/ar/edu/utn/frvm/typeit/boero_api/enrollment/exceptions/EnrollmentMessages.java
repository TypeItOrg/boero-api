package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

public final class EnrollmentMessages {

  public static final String ENROLLMENT_APPLICATION_NOT_FOUND =
      "La solicitud de inscripción especificada no existe.";
  public static final String ENROLLMENT_APPLICATION_NOT_EDITABLE =
      "La solicitud de inscripción ya no puede editarse.";
  public static final String ENROLLMENT_APPLICATION_DATA_REQUIRED =
      "Los datos del borrador son requeridos.";
  public static final String ENROLLMENT_APPLICATION_DATA_INVALID =
      "Los datos del borrador deben ser un objeto JSON válido.";
  public static final String ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID =
      "El trayecto formativo seleccionado no está habilitado para inscripción.";
  public static final String ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID =
      "Los espacios academicos seleccionados no son validos.";
  public static final String ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID =
      "Uno o mas espacios academicos seleccionados no estan habilitados para inscripcion.";
  public static final String ENROLLMENT_APPLICATION_INSTRUMENT_SELECTION_INVALID =
      "La seleccion de instrumentos no es valida.";
  public static final String ENROLLMENT_APPLICATION_INSTRUMENT_INVALID =
      "Uno o mas instrumentos seleccionados no estan habilitados para inscripcion.";
  public static final String ENROLLMENT_APPLICATION_APPLICANT_REQUIRED =
      "Solo un postulante puede operar sobre solicitudes de inscripción.";

  private EnrollmentMessages() {}
}
