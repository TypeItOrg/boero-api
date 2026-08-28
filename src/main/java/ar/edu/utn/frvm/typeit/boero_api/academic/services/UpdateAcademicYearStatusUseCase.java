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
  private final ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository
      courseRepository;

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
        && academicYearRepository.existsByInstitution_IdAndStatusAndDeletedAtIsNull(
            institutionId, AcademicYearStatus.ACTIVE)) {
      throw AcademicConflictException.forField(
          "status", AcademicMessages.ACADEMIC_YEAR_ACTIVE_CONFLICT);
    }
    final boolean closing = request.status() == AcademicYearStatus.CLOSED;
    try {
      academicYear.transitionTo(request.status());
      if (closing) {
        final var courses = courseRepository.findAllByAcademicYearId(id);
        for (final var course : courses) {
          if (course.getStatus()
              != ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.CLOSED) {
            course.close();
          }
        }
      }
      academicYearRepository.flush();
      if (closing) {
        courseRepository.flush();
      }
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
  }

  public long countActiveOrInactiveCoursesForYear(final UUID academicYearId) {
    return courseRepository.findAllByAcademicYearId(academicYearId).stream()
        .filter(c -> c.getDeletedAt() == null)
        .filter(
            c ->
                c.getStatus()
                    != ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.CLOSED)
        .count();
  }
}
