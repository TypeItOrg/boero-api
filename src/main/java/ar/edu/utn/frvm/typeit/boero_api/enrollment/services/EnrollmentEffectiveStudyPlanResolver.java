package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for which {@link StudyPlan} an enrollment application is effectively
 * working against. Read flows (listing spaces/instruments) resolve against the application's
 * current plan; the draft validator resolves against a candidate {@code trainingPathId} before
 * the applicant's career selection is actually persisted.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentEffectiveStudyPlanResolver {

  private final StudyPlanRepository studyPlanRepository;

  public StudyPlan resolve(final UUID institutionId, final EnrollmentApplication application) {
    return resolveForTrainingPath(institutionId, application, null);
  }

  public StudyPlan resolveForTrainingPath(
      final UUID institutionId,
      final EnrollmentApplication application,
      final @Nullable UUID trainingPathId) {
    if (trainingPathId == null) {
      return application.getStudyPlan();
    }

    final var validOn =
        application.getAcademicYear().getStartDate() != null
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
