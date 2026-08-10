package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StudyPlanDraftGuard {

  private final StudyPlanRepository studyPlanRepository;

  StudyPlan lock(final UUID institutionId, final UUID studyPlanId) {
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForUpdate(studyPlanId, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    plan.ensureDraft();
    return plan;
  }
}
