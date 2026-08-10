package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class InstrumentNotFoundException extends AcademicNotFoundException {
  public InstrumentNotFoundException() {
    super(AcademicMessages.INSTRUMENT_NOT_FOUND);
  }
}
