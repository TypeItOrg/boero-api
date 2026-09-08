package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicSpaceResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateAcademicSpaceRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAcademicSpaceUseCase {
  private final AcademicSpaceRepository academicSpaceRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public AcademicSpaceResponse execute(
      final UUID institutionId, final CreateAcademicSpaceRequest request) {
    final var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    final var name = AcademicNameNormalizer.display(request.name());
    if (academicSpaceRepository.existsByNormalizedNameAndTypeAndFormat(
        institutionId, name, request.type().name(), request.format().name())) {
      throw AcademicConflictException.forField("name", AcademicMessages.DUPLICATE_NAME);
    }
    try {
      final var saved =
          academicSpaceRepository.save(
              AcademicSpace.create(
                  institution, name, request.description(), request.type(), request.format()));
      academicSpaceRepository.flush();
      return AcademicSpaceResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
