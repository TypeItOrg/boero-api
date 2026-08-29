package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicYearNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCourseStatusUseCase {
  private final CourseRepository courseRepository;
  private final AcademicYearRepository academicYearRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final CourseStatusRequest request) {
    final var academicYearId =
        courseRepository
            .findAcademicYearIdByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    final var academicYear =
        academicYearRepository
            .findByIdAndInstitution_IdForUpdate(academicYearId, institutionId)
            .orElseThrow(AcademicYearNotFoundException::new);
    final var course =
        courseRepository
            .findByIdAndInstitution_IdForUpdate(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (request.status() == CourseStatus.ACTIVE
        && academicYear.getStatus() != AcademicYearStatus.ACTIVE) {
      throw new AcademicConflictException(AcademicMessages.COURSE_YEAR_NOT_ACTIVE);
    }
    course.updateStatus(request.status());
    courseRepository.flush();
  }
}
