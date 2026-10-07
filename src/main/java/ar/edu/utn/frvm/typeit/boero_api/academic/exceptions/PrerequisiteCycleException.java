package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import java.io.Serial;

public class PrerequisiteCycleException extends AcademicConflictException {

  @Serial private static final long serialVersionUID = 1L;

  public PrerequisiteCycleException() {
    super(AcademicMessages.PREREQUISITE_CYCLE);
  }
}
