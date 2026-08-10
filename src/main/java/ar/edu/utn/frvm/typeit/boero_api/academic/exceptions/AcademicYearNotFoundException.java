package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class AcademicYearNotFoundException extends AcademicNotFoundException {
  public AcademicYearNotFoundException() {
    super(AcademicMessages.ACADEMIC_YEAR_NOT_FOUND);
  }
}
