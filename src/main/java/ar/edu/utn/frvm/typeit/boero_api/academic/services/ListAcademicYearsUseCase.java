package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicYearRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicYearResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAcademicYearsUseCase {

  private static final Map<AcademicYearStatus, String> STATUS_LABELS =
      Map.of(
          AcademicYearStatus.PLANNED, "Planificado",
          AcademicYearStatus.ACTIVE, "Activo",
          AcademicYearStatus.CLOSED, "Cerrado");

  private static final String DISPLAY_DATE_FORMAT = "DD/MM/YYYY";
  private static final String ISO_DATE_FORMAT = "YYYY-MM-DD";
  private static final String YEAR_FORMAT = "FM9999999990";

  private final AcademicYearRepository academicYearRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicYearResponse> execute(
      final UUID institutionId,
      final String search,
      final AcademicYearStatus status,
      final Integer year,
      final LocalDate startDate,
      final LocalDate endDate,
      final LocalDate validOn,
      final Pageable pageable) {
    final var page =
        academicYearRepository.findAll(
            byFilters(institutionId, search, status, year, startDate, endDate, validOn), pageable);
    return PaginatedResponse.from(page.map(AcademicYearResponse::from));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicYearResponse> execute(
      final UUID institutionId,
      final String search,
      final AcademicYearStatus status,
      final Integer year,
      final LocalDate startDate,
      final LocalDate endDate,
      final Pageable pageable) {
    return execute(institutionId, search, status, year, startDate, endDate, null, pageable);
  }

  private static Specification<AcademicYear> byFilters(
      final UUID institutionId,
      final String search,
      final AcademicYearStatus status,
      final Integer year,
      final LocalDate startDate,
      final LocalDate endDate,
      final LocalDate validOn) {
    return (root, query, criteriaBuilder) -> {
      final List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.equal(root.get("institution").get("id"), institutionId));
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      if (year != null) {
        predicates.add(criteriaBuilder.equal(root.get("year"), year));
      }
      if (startDate != null) {
        predicates.add(criteriaBuilder.equal(root.get("startDate"), startDate));
      }
      if (endDate != null) {
        predicates.add(criteriaBuilder.equal(root.get("endDate"), endDate));
      }
      if (validOn != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), validOn));
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), validOn));
      }
      final String normalizedSearch = SearchNormalization.normalizeSearch(search);
      if (normalizedSearch != null) {
        predicates.add(searchPredicate(root, criteriaBuilder, normalizedSearch));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static Predicate searchPredicate(
      final Root<AcademicYear> root, final CriteriaBuilder criteriaBuilder, final String search) {
    final String pattern = SearchNormalization.likeContainsPattern(search);
    final List<Predicate> matches = new ArrayList<>();
    matches.add(
        like(
            criteriaBuilder,
            SearchNormalization.unaccentLower(
                criteriaBuilder,
                criteriaBuilder.function(
                    "to_char",
                    String.class,
                    root.get("year"),
                    criteriaBuilder.literal(YEAR_FORMAT))),
            pattern));
    matches.add(
        like(
            criteriaBuilder,
            SearchNormalization.unaccentLower(
                criteriaBuilder,
                criteriaBuilder.function(
                    "to_char",
                    String.class,
                    root.get("startDate"),
                    criteriaBuilder.literal(DISPLAY_DATE_FORMAT))),
            pattern));
    matches.add(
        like(
            criteriaBuilder,
            SearchNormalization.unaccentLower(
                criteriaBuilder,
                criteriaBuilder.function(
                    "to_char",
                    String.class,
                    root.get("startDate"),
                    criteriaBuilder.literal(ISO_DATE_FORMAT))),
            pattern));
    matches.add(
        like(
            criteriaBuilder,
            SearchNormalization.unaccentLower(
                criteriaBuilder,
                criteriaBuilder.function(
                    "to_char",
                    String.class,
                    root.get("endDate"),
                    criteriaBuilder.literal(DISPLAY_DATE_FORMAT))),
            pattern));
    matches.add(
        like(
            criteriaBuilder,
            SearchNormalization.unaccentLower(
                criteriaBuilder,
                criteriaBuilder.function(
                    "to_char",
                    String.class,
                    root.get("endDate"),
                    criteriaBuilder.literal(ISO_DATE_FORMAT))),
            pattern));

    final Set<AcademicYearStatus> matchingStatuses = matchingStatuses(search);
    if (!matchingStatuses.isEmpty()) {
      matches.add(root.get("status").in(matchingStatuses));
    }
    return criteriaBuilder.or(matches.toArray(Predicate[]::new));
  }

  private static Predicate like(
      final CriteriaBuilder criteriaBuilder,
      final Expression<String> expression,
      final String pattern) {
    return criteriaBuilder.like(expression, pattern, '\\');
  }

  private static Set<AcademicYearStatus> matchingStatuses(final String search) {
    final String normalizedSearch = SearchNormalization.normalizeForComparison(search);
    return Arrays.stream(AcademicYearStatus.values())
        .filter(
            status ->
                SearchNormalization.normalizeForComparison(status.name()).contains(normalizedSearch)
                    || SearchNormalization.normalizeForComparison(STATUS_LABELS.get(status))
                        .contains(normalizedSearch))
        .collect(Collectors.toUnmodifiableSet());
  }
}
