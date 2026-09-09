package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceInstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDraftData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentDraftDataValidator {

  private final TrainingPathRepository trainingPathRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanSpaceInstrumentRepository studyPlanSpaceInstrumentRepository;

  public void validate(
      final UUID institutionId,
      final EnrollmentApplication application,
      final EnrollmentDraftData data) {
    if (data == null) {
      return;
    }

    UUID trainingPathId = null;
    if (data.getCareerSelection() != null && data.getCareerSelection().getTrainingPathId() != null) {
      trainingPathId = data.getCareerSelection().getTrainingPathId();
      trainingPathRepository
          .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(trainingPathId, institutionId)
          .orElseThrow(TrainingPathNotFoundException::new);
    }

    final UUID effectiveStudyPlanId =
        resolveEffectiveStudyPlanId(institutionId, application, trainingPathId);

    Set<UUID> selectedSpaceIds = Set.of();
    if (data.getAcademicSpaceSelection() != null
        && data.getAcademicSpaceSelection().getStudyPlanSpaceIds() != null) {
      selectedSpaceIds =
          validateStudyPlanSpaces(
              institutionId,
              effectiveStudyPlanId,
              data.getAcademicSpaceSelection().getStudyPlanSpaceIds());
    }

    if (data.getInstrumentSelection() != null
        && data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds() != null) {
      validateInstruments(
          institutionId,
          selectedSpaceIds,
          data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds());
    }
  }

  private Set<UUID> validateStudyPlanSpaces(
      final UUID institutionId, final UUID studyPlanId, final List<UUID> studyPlanSpaceIds) {
    if (studyPlanSpaceIds.isEmpty()) {
      return Set.of();
    }
    final Set<UUID> uniqueIds = new HashSet<>(studyPlanSpaceIds);
    if (uniqueIds.size() != studyPlanSpaceIds.size()) {
      throw invalidStudyPlanSpaces();
    }
    final int eligibleCount =
        studyPlanSpaceRepository
            .findEligibleByIdInAndStudyPlanId(institutionId, studyPlanId, studyPlanSpaceIds)
            .size();
    if (eligibleCount != studyPlanSpaceIds.size()) {
      throw new EnrollmentValidationException(
          EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID,
          Map.of(
              "academicSpaceSelection.studyPlanSpaceIds",
              EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID));
    }
    return uniqueIds;
  }

  private void validateInstruments(
      final UUID institutionId,
      final Set<UUID> selectedSpaceIds,
      final Map<UUID, UUID> studyPlanSpaceInstrumentIds) {
    if (studyPlanSpaceInstrumentIds.isEmpty()) {
      return;
    }

    final var allowedRelations =
        studyPlanSpaceInstrumentRepository.findActiveByStudyPlanSpaceIds(
            institutionId, new ArrayList<>(selectedSpaceIds));
    final Map<UUID, Set<UUID>> allowedInstrumentIdsBySpaceId = new HashMap<>();
    for (final var relation : allowedRelations) {
      allowedInstrumentIdsBySpaceId
          .computeIfAbsent(relation.getStudyPlanSpace().getId(), ignored -> new HashSet<>())
          .add(relation.getInstrument().getId());
    }

    for (final var entry : studyPlanSpaceInstrumentIds.entrySet()) {
      final UUID studyPlanSpaceId = entry.getKey();
      final UUID instrumentId = entry.getValue();

      if (!selectedSpaceIds.contains(studyPlanSpaceId)) {
        throw invalidInstrumentSelection();
      }

      final Set<UUID> allowed =
          allowedInstrumentIdsBySpaceId.getOrDefault(studyPlanSpaceId, Set.of());
      if (allowed.isEmpty() || !allowed.contains(instrumentId)) {
        throw new EnrollmentValidationException(
            EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_INVALID,
            Map.of(
                "instrumentSelection.studyPlanSpaceInstrumentIds",
                EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_INVALID));
      }
    }
  }

  private EnrollmentValidationException invalidStudyPlanSpaces() {
    return new EnrollmentValidationException(
        EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID,
        Map.of(
            "academicSpaceSelection.studyPlanSpaceIds",
            EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID));
  }

  private EnrollmentValidationException invalidInstrumentSelection() {
    return new EnrollmentValidationException(
        EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_SELECTION_INVALID,
        Map.of(
            "instrumentSelection.studyPlanSpaceInstrumentIds",
            EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_SELECTION_INVALID));
  }

  private UUID resolveEffectiveStudyPlanId(
      final UUID institutionId,
      final EnrollmentApplication application,
      final UUID trainingPathId) {
    if (trainingPathId == null) {
      return application.getStudyPlan().getId();
    }

    final var validOn =
        application.getAcademicYear().getStartDate() != null
            ? application.getAcademicYear().getStartDate()
            : java.time.LocalDate.now();

    return studyPlanRepository
        .findActiveByTrainingPathIdAndInstitutionIdValidOn(
            trainingPathId,
            institutionId,
            ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus.ACTIVE,
            validOn)
        .stream()
        .findFirst()
        .orElseThrow(
            ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException::new)
        .getId();
  }
}
