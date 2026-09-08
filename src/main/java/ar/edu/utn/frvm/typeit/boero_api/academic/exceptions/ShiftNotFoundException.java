package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class ShiftNotFoundException extends AcademicNotFoundException {
  public ShiftNotFoundException() {
    super(AcademicMessages.SHIFT_NOT_FOUND);
  }
}
