package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class AcademicSpaceNotFoundException extends AcademicNotFoundException {
  public AcademicSpaceNotFoundException() {
    super(AcademicMessages.ACADEMIC_SPACE_NOT_FOUND);
  }
}
