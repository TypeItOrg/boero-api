package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicYearResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.UpdateAcademicYearRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAcademicYearUseCase {

  private final AcademicYearRepository academicYearRepository;

  @Transactional
  public AcademicYearResponse execute(
      final UUID institutionId, final UUID id, final UpdateAcademicYearRequest request) {
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    if (academicYearRepository.existsByInstitution_IdAndYearAndIdNot(
        institutionId, request.year(), id)) {
      throw AcademicConflictException.forField("year", AcademicMessages.DUPLICATE_YEAR);
    }
    academicYear.update(request.year(), request.startDate(), request.endDate());
    updateStatus(institutionId, academicYear, request.status());
    try {
      academicYearRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return AcademicYearResponse.from(academicYear);
  }

  private void updateStatus(
      final UUID institutionId,
      final AcademicYear academicYear,
      final AcademicYearStatus targetStatus) {
    if (targetStatus == null || targetStatus == academicYear.getStatus()) return;
    if (targetStatus == AcademicYearStatus.ACTIVE
        && (academicYear.getStartDate() == null || academicYear.getEndDate() == null)) {
      throw new AcademicValidationException(AcademicMessages.ACADEMIC_YEAR_DATES_REQUIRED);
    }
    if (targetStatus == AcademicYearStatus.ACTIVE
        && academicYearRepository.existsByInstitution_IdAndStatus(
            institutionId, AcademicYearStatus.ACTIVE)) {
      throw AcademicConflictException.forField(
          "status", AcademicMessages.ACADEMIC_YEAR_ACTIVE_CONFLICT);
    }
    academicYear.transitionTo(targetStatus);
  }
}
