package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class PrerequisiteNotFoundException extends AcademicNotFoundException {
  public PrerequisiteNotFoundException() {
    super(AcademicMessages.PREREQUISITE_NOT_FOUND);
  }
}
