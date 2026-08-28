package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicConflictException;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.StudyPlanNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.StudyPlanSpaceRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseSpaceOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCourseSpaceOptionsUseCase {
  private final StudyPlanRepository studyPlanRepository;
  private final StudyPlanSpaceRepository studyPlanSpaceRepository;

  @Transactional(readOnly = true)
  public List<CourseSpaceOptionResponse> execute(
      final UUID institutionId, final UUID studyPlanId, final @Nullable String search) {
    final var plan =
        studyPlanRepository
            .findByIdAndInstitution_Id(studyPlanId, institutionId)
            .orElseThrow(StudyPlanNotFoundException::new);
    if (plan.getStatus() != StudyPlanStatus.ACTIVE) {
      throw new AcademicConflictException(AcademicMessages.COURSE_STUDY_PLAN_NOT_ACTIVE);
    }
    final var normalizedSearch =
        search == null ? null : AcademicNameNormalizer.search(search).toLowerCase();
    final var spaces = new LinkedHashMap<UUID, CourseSpaceOptionResponse>();
    studyPlanSpaceRepository
        .findByStudyPlanIdWithDetails(studyPlanId)
        .forEach(
            space -> {
              final var academicSpace = space.getAcademicSpace();
              if (normalizedSearch != null
                  && !academicSpace.getName().toLowerCase().contains(normalizedSearch)) {
                return;
              }
              spaces.putIfAbsent(
                  academicSpace.getId(), CourseSpaceOptionResponse.from(academicSpace));
            });
    return List.copyOf(spaces.values());
  }
}
