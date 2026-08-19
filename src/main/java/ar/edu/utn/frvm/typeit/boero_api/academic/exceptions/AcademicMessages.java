package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public final class AcademicMessages {

  public static final String ACADEMIC_YEAR_NOT_FOUND = "El ciclo lectivo especificado no existe.";
  public static final String TRAINING_PATH_NOT_FOUND =
      "El trayecto formativo especificado no existe.";
  public static final String STUDY_PLAN_NOT_FOUND = "El plan de estudio especificado no existe.";
  public static final String ACADEMIC_LEVEL_NOT_FOUND =
      "El nivel académico especificado no existe.";
  public static final String ACADEMIC_SPACE_NOT_FOUND =
      "El espacio académico especificado no existe.";
  public static final String INSTRUMENT_NOT_FOUND = "El instrumento especificado no existe.";
  public static final String STUDY_PLAN_SPACE_NOT_FOUND =
      "El espacio del plan especificado no existe.";
  public static final String PREREQUISITE_NOT_FOUND = "La correlatividad especificada no existe.";
  public static final String DUPLICATE_NAME =
      "Ya existe un recurso con ese nombre en la institución.";
  public static final String DUPLICATE_YEAR = "Ya existe un ciclo lectivo para ese año.";
  public static final String DUPLICATE_ORDER = "El orden indicado ya está en uso.";
  public static final String DUPLICATE_PLAN_SPACE =
      "El espacio académico ya está incorporado en ese nivel del plan.";
  public static final String DUPLICATE_PREREQUISITE =
      "La correlatividad ya está configurada para esa instancia.";
  public static final String STUDY_PLAN_SPACE_HAS_PREREQUISITES =
      "El espacio no puede eliminarse porque participa en una correlatividad. Eliminá las correlatividades asociadas antes de continuar.";
  public static final String STUDY_PLAN_CURRICULUM_REQUIRES_DRAFT =
      "La estructura curricular solo puede modificarse mientras el plan de estudio está en borrador.";
  public static final String ACADEMIC_LEVEL_HAS_SPACES =
      "El nivel no puede eliminarse porque contiene espacios académicos. Quitá esos espacios antes de continuar.";
  public static final String TRAINING_PATH_HAS_ACTIVE_PLANS =
      "El trayecto formativo no puede desactivarse porque tiene planes de estudio activos asociados.";
  public static final String ACADEMIC_SPACE_HAS_ACTIVE_OR_DRAFT_PLANS =
      "El espacio académico no puede desactivarse porque está utilizado en un plan de estudio activo o en borrador.";
  public static final String STUDY_PLAN_ACTIVATION_REQUIRES_START_AND_ACTIVE_PATH =
      "El plan no puede activarse hasta completar la fecha de inicio y tener un trayecto formativo activo.";
  public static final String STUDY_PLAN_ACTIVATION_REQUIRES_SPACES =
      "El plan no puede activarse porque todavía no tiene espacios académicos incorporados.";
  public static final String STUDY_PLAN_STATUS_TRANSITION_INVALID =
      "El plan de estudio solo puede volver a borrador desde un estado válido.";
  public static final String INVALID_STATE = "La transición de estado solicitada no es válida.";
  public static final String INVALID_RELATIONSHIP =
      "La relación académica especificada no es válida.";
  public static final String PREREQUISITE_CYCLE = "La correlatividad solicitada genera un ciclo.";
  public static final String ACADEMIC_YEAR_ACTIVE_CONFLICT =
      "La institución ya tiene un ciclo lectivo activo.";
  public static final String ACADEMIC_YEAR_DATES_REQUIRED =
      "Las fechas de inicio y fin son requeridas para activar el ciclo lectivo.";
  public static final String ACADEMIC_YEAR_DATES_INVALID =
      "Las fechas del ciclo lectivo no son válidas.";
  public static final String ACADEMIC_YEAR_OUT_OF_RANGE =
      "El año debe estar entre 2000 y el año siguiente al actual.";
  public static final String ACADEMIC_YEAR_START_DATE_INVALID =
      "La fecha de inicio debe pertenecer al año del ciclo lectivo.";
  public static final String ACADEMIC_YEAR_END_DATE_INVALID =
      "La fecha de finalización debe pertenecer al año del ciclo lectivo o al siguiente.";
  public static final String STUDY_PLAN_END_DATE_REQUIRED =
      "La fecha final es requerida al inactivar el plan.";
  public static final String STUDY_PLAN_END_DATE_INVALID =
      "La fecha final no puede ser anterior al inicio del plan.";
  public static final String STUDY_PLAN_DATES_INVALID = "La vigencia del plan no es válida.";
  public static final String STUDY_PLAN_START_DATE_REQUIRED =
      "Completá la fecha de inicio antes de indicar una fecha final.";
  public static final String DATE_PAIR_REQUIRED = "Completá ambas fechas o dejá ambas vacías.";
  public static final String INVALID_VALUE = "El valor indicado no es válido.";
  public static final String INVALID_NAME_FORMAT = "El nombre no tiene un formato válido.";
  public static final String INVALID_DISPLAY_ORDER = "El orden debe ser mayor que cero.";
  public static final String DELETE_REFERENCED_RESOURCE =
      "El recurso no puede eliminarse porque todavía tiene elementos vigentes asociados.";
  public static final String RESTORE_PARENT_UNAVAILABLE =
      "El recurso no puede restaurarse porque su elemento superior no está disponible.";
  public static final String RESTORE_CONFLICT =
      "El recurso no puede restaurarse porque sus datos ya están en uso.";

  private AcademicMessages() {}
}
