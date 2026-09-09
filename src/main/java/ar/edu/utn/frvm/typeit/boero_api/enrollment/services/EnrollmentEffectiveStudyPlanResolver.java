package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentEffectiveStudyPlanResolver {

  public StudyPlan resolve(final UUID institutionId, final EnrollmentApplication application) {
    return application.getStudyPlan();
  }
}
