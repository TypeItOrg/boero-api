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
  public static final String ENROLLMENT_APPLICATION_SPACES_REQUIRED =
      "Debe seleccionar al menos un espacio académico para enviar la inscripción.";

  public static final String APPLICATION_NOT_FOUND =
      "La solicitud de inscripción no existe o no pertenece a la institución.";
  public static final String APPLICATION_NOT_OWNED =
      "La solicitud de inscripción no pertenece al postulante.";
  public static final String APPLICATION_ALREADY_RESOLVED =
      "La solicitud de inscripción ya fue resuelta y no puede modificarse.";
  public static final String APPLICATION_NOT_PENDING_EVALUATION =
      "Solo las solicitudes con estado 'Pendiente de evaluación' pueden resolverse.";
  public static final String APPLICATION_NOT_EDITABLE =
      "La solicitud de inscripción no se encuentra en estado editable.";
  public static final String APPLICATION_CANNOT_SUBMIT =
      "La solicitud de inscripción no puede enviarse en su estado actual.";
  public static final String PERIOD_CLOSED =
      "No existe un período de inscripción habilitado para el ciclo lectivo seleccionado.";
  public static final String REJECTION_REASON_REQUIRED =
      "Debe indicar un motivo para rechazar la solicitud.";

  private EnrollmentMessages() {}
}
