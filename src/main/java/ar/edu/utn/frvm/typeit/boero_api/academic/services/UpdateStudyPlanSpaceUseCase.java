package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpaceInstrument;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceInstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceInstrumentOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateStudyPlanSpaceRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStudyPlanSpaceUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final AcademicSpaceRepository academicSpaceRepository;
  private final AcademicLevelRepository academicLevelRepository;
  private final InstrumentRepository instrumentRepository;
  private final StudyPlanSpaceInstrumentRepository studyPlanSpaceInstrumentRepository;
  private final StudyPlanDraftGuard studyPlanDraftGuard;

  @Transactional
  public StudyPlanSpaceResponse execute(
      final UUID institutionId, final UUID id, final UpdateStudyPlanSpaceRequest request) {
    final var existing =
        studyPlanSpaceRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(StudyPlanSpaceNotFoundException::new);
    final var plan = studyPlanDraftGuard.lock(institutionId, existing.getStudyPlan().getId());
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_IdAndActiveTrueAndDeletedAtIsNull(
                request.academicSpaceId(), institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    final var level = resolveLevel(plan.getId(), request.academicLevelId());
    final var instruments = resolveInstruments(institutionId, request.instrumentIds());
    existing.update(
        space, level, request.requirementType(), request.displayOrder(), request.approvalMode());
    try {
      studyPlanSpaceInstrumentRepository.deleteByStudyPlanSpace_Id(existing.getId());
      saveInstrumentRelations(existing, instruments);
      studyPlanSpaceRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return StudyPlanSpaceResponse.from(existing, instrumentOptions(instruments));
  }

  private AcademicLevel resolveLevel(final UUID studyPlanId, final UUID academicLevelId) {
    if (academicLevelId == null) {
      return null;
    }
    return academicLevelRepository
        .findByIdAndStudyPlan_Id(academicLevelId, studyPlanId)
        .orElseThrow(() -> new AcademicConflictException(AcademicMessages.INVALID_RELATIONSHIP));
  }

  private List<Instrument> resolveInstruments(
      final UUID institutionId, final List<UUID> instrumentIds) {
    if (instrumentIds == null || instrumentIds.isEmpty()) {
      return List.of();
    }

    final var uniqueIds = new LinkedHashSet<>(instrumentIds);
    if (uniqueIds.size() != instrumentIds.size()) {
      throw new AcademicConflictException(AcademicMessages.INVALID_RELATIONSHIP);
    }

    final List<Instrument> instruments = new ArrayList<>();
    for (final UUID instrumentId : uniqueIds) {
      final var instrument =
          instrumentRepository
              .findByIdAndInstitution_Id(instrumentId, institutionId)
              .filter(Instrument::isActive)
              .orElseThrow(
                  () -> new AcademicConflictException(AcademicMessages.INVALID_RELATIONSHIP));
      instruments.add(instrument);
    }
    return instruments;
  }

  private void saveInstrumentRelations(
      final ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace studyPlanSpace,
      final List<Instrument> instruments) {
    for (final Instrument instrument : instruments) {
      studyPlanSpaceInstrumentRepository.save(
          StudyPlanSpaceInstrument.create(
              studyPlanSpace.getInstitution(), studyPlanSpace, instrument));
    }
  }

  private List<StudyPlanSpaceInstrumentOptionResponse> instrumentOptions(
      final List<Instrument> instruments) {
    return instruments.stream()
        .map(
            instrument ->
                new StudyPlanSpaceInstrumentOptionResponse(
                    instrument.getId(), instrument.getName()))
        .toList();
  }
}
