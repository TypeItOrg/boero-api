package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class TrainingPathNotFoundException extends AcademicNotFoundException {
  public TrainingPathNotFoundException() {
    super(AcademicMessages.TRAINING_PATH_NOT_FOUND);
  }
}
