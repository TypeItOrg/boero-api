package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.CourseNotFoundException;
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

  @Transactional
  public void execute(final UUID institutionId, final UUID id, final CourseStatusRequest request) {
    final var course =
        courseRepository
            .findByIdAndInstitution_Id(id, institutionId)
            .orElseThrow(CourseNotFoundException::new);
    course.updateStatus(request.status());
    courseRepository.flush();
  }
}
