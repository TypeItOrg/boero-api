package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicSpaceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateAcademicSpaceRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAcademicSpaceUseCase {
  private final AcademicSpaceRepository academicSpaceRepository;

  @Transactional
  public AcademicSpaceResponse execute(
      final UUID institutionId, final UUID id, final UpdateAcademicSpaceRequest request) {
    final var space =
        academicSpaceRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(AcademicSpaceNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (academicSpaceRepository.existsByNormalizedNameAndTypeAndIdNot(
        institutionId, name, request.type().name(), id)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    space.update(name, request.description(), request.type());
    try {
      academicSpaceRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return AcademicSpaceResponse.from(space);
  }
}
