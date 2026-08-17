package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicYearResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CreateAcademicYearRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAcademicYearUseCase {

  private final AcademicYearRepository academicYearRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public AcademicYearResponse execute(
      final UUID institutionId, final CreateAcademicYearRequest request) {
    final var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    if (academicYearRepository.existsByInstitution_IdAndYearAndDeletedAtIsNull(
        institutionId, request.year())) {
      throw AcademicConflictException.forField("year", AcademicMessages.DUPLICATE_YEAR);
    }
    try {
      final var saved =
          academicYearRepository.save(
              AcademicYear.create(
                  institution, request.year(), request.startDate(), request.endDate()));
      academicYearRepository.flush();
      return AcademicYearResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
