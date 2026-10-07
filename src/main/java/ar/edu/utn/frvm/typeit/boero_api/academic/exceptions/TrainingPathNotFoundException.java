package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import java.io.Serial;

public class TrainingPathNotFoundException extends AcademicNotFoundException {

  @Serial private static final long serialVersionUID = 1L;

  public TrainingPathNotFoundException() {
    super(AcademicMessages.TRAINING_PATH_NOT_FOUND);
  }
}
