package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.CourseStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicMessages;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicValidationException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.CourseRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.CourseResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCoursesUseCase {
  private final CourseRepository courseRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<CourseResponse> execute(
      final @Nullable UUID institutionId,
      final String search,
      final CourseStatus status,
      final @Nullable UUID academicSpaceId,
      final @Nullable UUID trainingPathId,
      final @Nullable UUID studyPlanId,
      final @Nullable Integer year,
      final boolean deleted,
      final Pageable pageable) {
    final Pageable repositoryPageable = mapSort(pageable);
    return PaginatedResponse.from(
        courseRepository
            .findByFilters(
                institutionId,
                AcademicNameNormalizer.search(search),
                status,
                academicSpaceId,
                trainingPathId,
                studyPlanId,
                year,
                deleted,
                repositoryPageable)
            .map(CourseResponse::from));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<CourseResponse> enrollmentOptions(
      UUID institutionId, Pageable pageable) {
    return PaginatedResponse.from(
        courseRepository
            .findEnrollmentOptions(
                institutionId, null, CourseStatus.ACTIVE, null, null, null, null, false, pageable)
            .map(CourseResponse::from));
  }

  private static final Map<String, String> SORT_FIELDS =
      Map.of(
          "institution.name",
          "institution.name",
          "academicSpace.name",
          "studyPlanSpace.academicSpace.name",
          "trainingPathName",
          "studyPlanSpace.studyPlan.trainingPath.name",
          "studyPlanName",
          "studyPlanSpace.studyPlan.name",
          "academicYear",
          "academicYear.year");

  private static Pageable mapSort(final Pageable pageable) {
    final Sort sort =
        Sort.by(pageable.getSort().stream().map(ListCoursesUseCase::mapOrder).toList());
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  private static Sort.Order mapOrder(final Sort.Order order) {
    final String property = SORT_FIELDS.get(order.getProperty());
    if (property == null) {
      throw new AcademicValidationException(
          AcademicMessages.INVALID_SORT_FIELD, Map.of("sort", order.getProperty()));
    }
    return order.withProperty(property);
  }

  public PaginatedResponse<CourseResponse> execute(
      final UUID institutionId,
      final String search,
      final CourseStatus status,
      @Nullable final UUID academicSpaceId,
      @Nullable final UUID trainingPathId,
      @Nullable final UUID studyPlanId,
      @Nullable final Integer year,
      final Pageable pageable) {
    return execute(
        institutionId,
        search,
        status,
        academicSpaceId,
        trainingPathId,
        studyPlanId,
        year,
        false,
        pageable);
  }
}
