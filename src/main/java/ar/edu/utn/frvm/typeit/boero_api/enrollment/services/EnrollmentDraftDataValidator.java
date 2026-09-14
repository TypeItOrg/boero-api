package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.TrainingPathNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceInstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class EnrollmentDraftDataValidator {

  private final TrainingPathRepository trainingPathRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanSpaceInstrumentRepository studyPlanSpaceInstrumentRepository;
  private final EnrollmentEffectiveStudyPlanResolver enrollmentEffectiveStudyPlanResolver;

  /**
   * Validates the draft payload and resolves the study plan the application should effectively be
   * working against. The caller is responsible for reassigning {@code
   * application.setStudyPlan(...)} when the returned plan differs from the current one — this
   * method never mutates the application.
   */
  public StudyPlan validate(
      final UUID institutionId,
      final EnrollmentApplication application,
      final EnrollmentDraftData data) {
    if (data == null) {
      return application.getStudyPlan();
    }

    UUID trainingPathId = null;

    if (data.getCareerSelection() != null
        && data.getCareerSelection().getTrainingPathId() != null) {
      trainingPathId = data.getCareerSelection().getTrainingPathId();
      trainingPathRepository
          .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(trainingPathId, institutionId)
          .orElseThrow(TrainingPathNotFoundException::new);
    }
    trainingPathRepository
        .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(trainingPathId, institutionId)
        .orElseThrow(TrainingPathNotFoundException::new);
    validateStudyPlanSpaceSelection(institutionId, application, data, trainingPathId);
    return;

    final StudyPlan effectiveStudyPlan =
        enrollmentEffectiveStudyPlanResolver.resolveForTrainingPath(
            institutionId, application, trainingPathId);

    Set<UUID> selectedSpaceIds = Set.of();

    if (data.getAcademicSpaceSelection() != null
        && data.getAcademicSpaceSelection().getStudyPlanSpaceIds() != null) {
      selectedSpaceIds =
          validateStudyPlanSpaces(
              institutionId,
              effectiveStudyPlan.getId(),
              data.getAcademicSpaceSelection().getStudyPlanSpaceIds());
    }

    if (data.getInstrumentSelection() != null
        && data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds() != null) {
      validateInstruments(
          institutionId,
          selectedSpaceIds,
          data.getInstrumentSelection().getStudyPlanSpaceInstrumentIds());
    }

    return effectiveStudyPlan;
  }

  public void validateSubmission(final EnrollmentApplication application) {
    final UUID institutionId = application.getInstitution().getId();
    final var spaces = application.getSelectedSpaces();
    final var selectedIds =
        validateStudyPlanSpaces(
            institutionId,
            application.getStudyPlan().getId(),
            spaces.stream().map(space -> space.getStudyPlanSpace().getId()).toList());
    final Map<UUID, UUID> selectedInstruments = new HashMap<>();

    for (final var space : spaces) {
      if (space.getInstrument() != null) {
        selectedInstruments.put(space.getStudyPlanSpace().getId(), space.getInstrument().getId());
      }
    }

    validateInstruments(institutionId, selectedIds, selectedInstruments);
    final var requiredRelations =
        studyPlanSpaceInstrumentRepository.findActiveByStudyPlanSpaceIds(
            institutionId, new ArrayList<>(selectedIds));

    for (final var relation : requiredRelations) {
      if (!selectedInstruments.containsKey(relation.getStudyPlanSpace().getId())) {
        throw new EnrollmentValidationException(
            EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_REQUIRED,
            Map.of(
                "instrumentSelection.studyPlanSpaceInstrumentIds",
                EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_REQUIRED));
      }
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
              "data.academicSpaceSelection.studyPlanSpaceIds",
              EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACE_INVALID));
    }

    return uniqueIds;
  }

  private void validateInstrumentSelection(
      final UUID institutionId,
      final Set<UUID> selectedStudyPlanSpaceIds,
      final UUID studyPlanId,
      final JsonNode data) {
    final JsonNode instrumentSelectionNode =
        data.path("instrumentSelection").path("studyPlanSpaceInstrumentIds");
    if (instrumentSelectionNode.isMissingNode() || instrumentSelectionNode.isNull()) {
      return;
    }
    if (!instrumentSelectionNode.isObject()) {
      throw invalidInstrumentSelection();
    }

    final var allowedRelations =
        studyPlanSpaceInstrumentRepository.findActiveByStudyPlanSpaceIds(
            institutionId, new ArrayList<>(selectedSpaceIds));
    final Map<UUID, Set<UUID>> allowedInstrumentIdsBySpaceId = new HashMap<>();

    for (final var relation : allowedRelations) {
      allowedInstrumentIdsByStudyPlanSpaceId
          .computeIfAbsent(relation.getStudyPlanSpace().getId(), ignored -> new HashSet<>())
          .add(relation.getInstrument().getId());
    }

    for (final var entry : instrumentSelectionNode.properties()) {
      final String studyPlanSpaceIdText = entry.getKey();
      final UUID studyPlanSpaceId;
      try {
        studyPlanSpaceId = UUID.fromString(studyPlanSpaceIdText);
      } catch (IllegalArgumentException exception) {
        throw invalidInstrumentSelection();
      }
      if (!selectedStudyPlanSpaceIds.contains(studyPlanSpaceId)) {
        throw invalidInstrumentSelection();
      }

      final Set<UUID> allowed =
          allowedInstrumentIdsBySpaceId.getOrDefault(studyPlanSpaceId, Set.of());

      if (allowed.isEmpty() || !allowed.contains(instrumentId)) {
        throw new EnrollmentValidationException(
            EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_INVALID,
            Map.of(
                "data.instrumentSelection.studyPlanSpaceInstrumentIds",
                EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_INVALID));
      }
    }
  }

  private EnrollmentValidationException invalidStudyPlanSpaces() {
    return new EnrollmentValidationException(
        EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID,
        Map.of(
            "data.academicSpaceSelection.studyPlanSpaceIds",
            EnrollmentMessages.ENROLLMENT_APPLICATION_STUDY_PLAN_SPACES_INVALID));
  }

  private EnrollmentValidationException invalidInstrumentSelection() {
    return new EnrollmentValidationException(
        EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_SELECTION_INVALID,
        Map.of(
            "data.instrumentSelection.studyPlanSpaceInstrumentIds",
            EnrollmentMessages.ENROLLMENT_APPLICATION_INSTRUMENT_SELECTION_INVALID));
  }
}
