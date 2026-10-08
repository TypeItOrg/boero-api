package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.InstrumentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.InstrumentRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ActiveStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstrumentStatusUseCase {
  private final InstrumentRepository instrumentRepository;
  private final CourseRepository courseRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final ActiveStatusRequest request) {
    final var instrument =
        instrumentRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(InstrumentNotFoundException::new);
    if (!request.active() && courseRepository.existsActiveByInstrument(institutionId, id)) {
      throw new AcademicConflictException(AcademicMessages.INSTRUMENT_IN_USE);
    }
    instrument.updateStatus(request.active());
    instrumentRepository.flush();
  }
}
