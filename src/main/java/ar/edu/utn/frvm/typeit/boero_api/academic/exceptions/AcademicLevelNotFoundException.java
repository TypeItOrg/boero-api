package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class AcademicLevelNotFoundException extends AcademicNotFoundException {
  public AcademicLevelNotFoundException() {
    super(AcademicMessages.ACADEMIC_LEVEL_NOT_FOUND);
  }
}
