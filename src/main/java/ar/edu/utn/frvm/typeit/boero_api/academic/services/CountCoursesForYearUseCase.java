package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CountCoursesForYearUseCase {
  private final CourseRepository courseRepository;

  @Transactional(readOnly = true)
  public long execute(final UUID academicYearId) {
    return courseRepository.countByAcademicYearIdAndStatusNot(academicYearId, CourseStatus.CLOSED);
  }
}
