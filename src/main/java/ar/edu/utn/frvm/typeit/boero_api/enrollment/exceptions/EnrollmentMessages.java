package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

public final class EnrollmentMessages {

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
