package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.ShiftNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.ShiftRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ShiftResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateShiftRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateShiftUseCase {
  private final ShiftRepository shiftRepository;

  @Transactional
  public ShiftResponse execute(
      final UUID institutionId, final UUID id, final UpdateShiftRequest request) {
    final var shift =
        shiftRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(ShiftNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (shiftRepository.existsByNormalizedNameAndIdNot(institutionId, name, id)) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    shift.update(name, request.description());
    try {
      shiftRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return ShiftResponse.from(shift);
  }
}
