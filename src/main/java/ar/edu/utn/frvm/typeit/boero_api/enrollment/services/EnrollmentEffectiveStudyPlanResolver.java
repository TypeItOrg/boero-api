package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentEffectiveStudyPlanResolver {

  private final StudyPlanRepository studyPlanRepository;

  public StudyPlan resolve(final UUID institutionId, final EnrollmentApplication application) {
    final JsonNode trainingPathNode =
        application.getData().path("careerSelection").path("trainingPathId");
    if (trainingPathNode.isMissingNode() || trainingPathNode.isNull()) {
      return application.getStudyPlan();
    }

    final UUID trainingPathId;
    try {
      trainingPathId = UUID.fromString(trainingPathNode.asText());
    } catch (IllegalArgumentException exception) {
      return application.getStudyPlan();
    }

    final LocalDate validOn = application.getAcademicYear().getStartDate() != null
        ? application.getAcademicYear().getStartDate()
        : LocalDate.now();

    return studyPlanRepository
        .findActiveByTrainingPathIdAndInstitutionIdValidOn(
            trainingPathId, institutionId, StudyPlanStatus.ACTIVE, validOn)
        .stream()
        .findFirst()
        .orElseThrow(StudyPlanNotFoundException::new);
  }
}
