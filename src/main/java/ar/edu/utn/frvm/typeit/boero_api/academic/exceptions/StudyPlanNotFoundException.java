package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class StudyPlanNotFoundException extends AcademicNotFoundException {
  public StudyPlanNotFoundException() {
    super(AcademicMessages.STUDY_PLAN_NOT_FOUND);
  }
}
