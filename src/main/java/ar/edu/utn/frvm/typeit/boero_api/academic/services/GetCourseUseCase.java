package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseUseCase {
  private final CourseRepository courseRepository;
  private final CourseTreeReader courseTreeReader;

  @Transactional(readOnly = true)
  public CourseResponse execute(final UUID institutionId, final UUID id) {
    final var course =
        courseRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    return CourseResponse.from(course, courseTreeReader.read(id));
  }
}
