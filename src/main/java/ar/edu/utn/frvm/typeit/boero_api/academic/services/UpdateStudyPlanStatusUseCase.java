package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanStatusRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStudyPlanStatusUseCase {
  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private final StudyPlanRepository studyPlanRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final CourseRepository courseRepository;

  @Transactional
  public void execute(
      final UUID institutionId, final UUID id, final StudyPlanStatusRequest request) {
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_IdForUpdate(id, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    if (request.status() == StudyPlanStatus.ACTIVE) {
      if (plan.getEffectiveFrom() == null || !plan.getTrainingPath().isActive()) {
        throw new AcademicConflictException(
            AcademicMessages.STUDY_PLAN_ACTIVATION_REQUIRES_START_AND_ACTIVE_PATH);
      }
      if (!studyPlanSpaceRepository.existsByStudyPlanId(id)) {
        throw new AcademicConflictException(AcademicMessages.STUDY_PLAN_ACTIVATION_REQUIRES_SPACES);
      }
      closePreviousVersion(plan);
      plan.activate();
      studyPlanRepository.flush();
      return;
    }
    if (request.status() == StudyPlanStatus.INACTIVE) {
      if (request.effectiveTo() == null) {
        throw new AcademicValidationException(AcademicMessages.STUDY_PLAN_END_DATE_REQUIRED);
      }
      if (request.effectiveTo().isAfter(LocalDate.now(ARGENTINA_TIME_ZONE))) {
        throw new AcademicValidationException(
            AcademicMessages.STUDY_PLAN_END_DATE_CANNOT_BE_FUTURE);
      }
      if (plan.getEffectiveFrom() != null
          && request.effectiveTo().isBefore(plan.getEffectiveFrom())) {
        throw new AcademicValidationException(AcademicMessages.STUDY_PLAN_END_DATE_INVALID);
      }
      if (courseRepository
          .existsByInstitution_IdAndStudyPlan_IdAndStatusNotClosedAndDeletedAtIsNull(
              institutionId, id)) {
        throw new AcademicConflictException(AcademicMessages.STUDY_PLAN_HAS_ACTIVE_COURSES);
      }
      plan.deactivate(request.effectiveTo());
      studyPlanRepository.flush();
      return;
    }
    if (request.status() != StudyPlanStatus.DRAFT || plan.getStatus() != StudyPlanStatus.DRAFT) {
      throw new AcademicConflictException(AcademicMessages.STUDY_PLAN_STATUS_TRANSITION_INVALID);
    }
  }

  private void closePreviousVersion(final StudyPlan plan) {
    final var previous = plan.getPreviousVersion();
    if (previous == null) {
      return;
    }
    if (plan.getEffectiveFrom() == null || previous.getEffectiveFrom() == null) {
      throw new AcademicConflictException(
          AcademicMessages.STUDY_PLAN_VERSION_REQUIRES_ORDERED_DATES);
    }
    final var lockedPrevious =
        studyPlanRepository
            .findByIdAndInstitution_IdForLifecycle(previous.getId(), plan.getInstitution().getId())
            .orElseThrow(StudyPlanNotFoundException::new);
    if (lockedPrevious.getStatus() == StudyPlanStatus.ACTIVE) {
      if (!plan.getEffectiveFrom().isAfter(lockedPrevious.getEffectiveFrom())) {
        throw new AcademicConflictException(AcademicMessages.STUDY_PLAN_VERSION_DATE_OVERLAP);
      }
      final var newVersionEnd = plan.getEffectiveFrom().minusDays(1);
      final var previousEnd = lockedPrevious.getEffectiveTo();
      lockedPrevious.deactivate(
          previousEnd != null && previousEnd.isBefore(newVersionEnd) ? previousEnd : newVersionEnd);
      return;
    }
    if (lockedPrevious.getStatus() != StudyPlanStatus.INACTIVE
        || lockedPrevious.getEffectiveTo() == null
        || !plan.getEffectiveFrom().isAfter(lockedPrevious.getEffectiveTo())) {
      throw new AcademicConflictException(AcademicMessages.STUDY_PLAN_VERSION_DATE_OVERLAP);
    }
  }
}
