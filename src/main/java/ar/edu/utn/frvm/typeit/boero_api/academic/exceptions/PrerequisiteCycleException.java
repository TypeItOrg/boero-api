package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class PrerequisiteCycleException extends AcademicConflictException {
  public PrerequisiteCycleException() {
    super(AcademicMessages.PREREQUISITE_CYCLE);
  }
}
