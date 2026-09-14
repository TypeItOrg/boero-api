package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceInstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceInstrumentOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.StudyPlanSpaceResponse;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStudyPlanSpacesUseCase {
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;
  private final StudyPlanRepository studyPlanRepository;
  private final StudyPlanSpaceInstrumentRepository studyPlanSpaceInstrumentRepository;

  @Transactional(readOnly = true)
  public List<StudyPlanSpaceResponse> execute(final UUID institutionId, final UUID studyPlanId) {
    studyPlanRepository
        .findByIdAndInstitution_Id(studyPlanId, institutionId)
        .orElseThrow(StudyPlanNotFoundException::new);
    final var instrumentsBySpace =
        studyPlanSpaceInstrumentRepository
            .findByStudyPlanIdWithInstruments(institutionId, studyPlanId)
            .stream()
            .collect(
                Collectors.groupingBy(
                    relation -> relation.getStudyPlanSpace().getId(),
                    Collectors.mapping(
                        relation ->
                            new StudyPlanSpaceInstrumentOptionResponse(
                                relation.getInstrument().getId(),
                                relation.getInstrument().getName()),
                        Collectors.toList())));

    return studyPlanSpaceRepository.findByStudyPlanIdWithDetails(studyPlanId).stream()
        .map(
            space ->
                StudyPlanSpaceResponse.from(
                    space, instrumentsBySpace.getOrDefault(space.getId(), List.of())))
        .toList();
  }
}
