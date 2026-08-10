package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicYearStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAcademicYearStatusUseCase {

  private final AcademicYearRepository academicYearRepository;

  @Transactional
  public void execute(
      final UUID institutionId, final UUID id, final AcademicYearStatusRequest request) {
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    if (request.status() == AcademicYearStatus.ACTIVE
        && (academicYear.getStartDate() == null || academicYear.getEndDate() == null)) {
      throw new AcademicValidationException(AcademicMessages.ACADEMIC_YEAR_DATES_REQUIRED);
    }
    if (request.status() == AcademicYearStatus.ACTIVE
        && academicYear.getStatus() != AcademicYearStatus.ACTIVE
        && academicYearRepository.existsByInstitution_IdAndStatus(
            institutionId, AcademicYearStatus.ACTIVE)) {
      throw AcademicConflictException.forField(
          "status", AcademicMessages.ACADEMIC_YEAR_ACTIVE_CONFLICT);
    }
    try {
      academicYear.transitionTo(request.status());
      academicYearRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }
}
