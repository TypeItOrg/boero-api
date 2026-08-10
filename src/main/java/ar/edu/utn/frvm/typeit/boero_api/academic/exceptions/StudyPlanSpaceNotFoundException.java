package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

public class StudyPlanSpaceNotFoundException extends AcademicNotFoundException {
  public StudyPlanSpaceNotFoundException() {
    super(AcademicMessages.STUDY_PLAN_SPACE_NOT_FOUND);
  }
}
