package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class CourseNotFoundException extends AcademicNotFoundException {
  public CourseNotFoundException() {
    super(AcademicMessages.COURSE_NOT_FOUND);
  }
}
