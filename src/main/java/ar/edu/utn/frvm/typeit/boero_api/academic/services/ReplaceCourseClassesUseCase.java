package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicIntegrityViolationTranslator;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseClassRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.ReplaceCourseClassesRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplaceCourseClassesUseCase {
  private final CourseRepository courseRepository;
  private final CourseClassRepository courseClassRepository;
  private final CourseClassAssembler courseClassAssembler;
  private final CourseTreeReader courseTreeReader;

  @Transactional
  public CourseResponse execute(
      final UUID institutionId, final UUID courseId, final ReplaceCourseClassesRequest request) {
    final var course =
        courseRepository
            .findByIdAndInstitution_IdForUpdate(courseId, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    if (course.getStatus() == ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus.CLOSED) {
      throw new ar.edu.utn.frvm.typeit.boero_api.academic.exceptions
          .InvalidAcademicStateException();
    }
    try {
      deleteCurrentClasses(courseId);
      courseClassAssembler.assemble(
          course.getInstitution(),
          course,
          course.getAcademicSpace().getFormat(),
          request.classes());
      courseClassRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw AcademicIntegrityViolationTranslator.translate(exception);
    }
    return CourseResponse.from(course, courseTreeReader.read(courseId));
  }

  private void deleteCurrentClasses(final UUID courseId) {
    courseClassRepository.deleteSchedulesByCourseId(courseId);
    courseClassRepository.deleteTeachersByCourseId(courseId);
    courseClassRepository.deleteDaysByCourseId(courseId);
    courseClassRepository.deleteByCourseId(courseId);
  }
}
