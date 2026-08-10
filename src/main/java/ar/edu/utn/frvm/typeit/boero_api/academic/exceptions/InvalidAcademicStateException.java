package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class InvalidAcademicStateException extends AcademicConflictException {
  public InvalidAcademicStateException() {
    super(AcademicMessages.INVALID_STATE);
  }
}
