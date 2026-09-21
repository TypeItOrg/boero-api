package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode.*;
import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.*;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicYearStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicSelectionOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.ScopedResourceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.PermissionAccess;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.ScopedAuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAcademicSelectionOptionsUseCase {
  private final EntityManager entityManager;
  private final ScopedAuthorizationService authorization;

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicSelectionOptionResponse> execute(
      UUID institutionId,
      String resource,
      String search,
      Boolean active,
      Boolean published,
      UUID trainingPathId,
      StudyPlanStatus status,
      Pageable pageable) {
    return execute(
        institutionId, resource, search, active, published, trainingPathId, status, pageable, null);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<AcademicSelectionOptionResponse> execute(
      UUID institutionId,
      String resource,
      String search,
      Boolean active,
      Boolean published,
      UUID trainingPathId,
      StudyPlanStatus status,
      Pageable pageable,
      PermissionCode operation) {
    String entity;
    Set<PermissionCode> permissions;
    String pathExpression = null;
    switch (resource) {
      case "training-paths" -> {
        entity = "TrainingPath";
        pathExpression = "item.id";
        permissions =
            Set.of(
                TRAINING_PATH_READ,
                STUDY_PLAN_READ,
                STUDY_PLAN_CREATE,
                STUDY_PLAN_UPDATE,
                COURSE_READ,
                COURSE_CREATE,
                ENROLLMENT_PERIOD_READ,
                ENROLLMENT_PERIOD_CREATE,
                ENROLLMENT_PERIOD_UPDATE,
                ENROLLMENT_APPLICATION_READ,
                COURSE_ENROLLMENT_READ);
      }
      case "study-plans" -> {
        entity = "StudyPlan";
        pathExpression = "item.trainingPath.id";
        permissions =
            Set.of(
                STUDY_PLAN_READ,
                COURSE_CREATE,
                COURSE_UPDATE,
                COURSE_READ,
                ENROLLMENT_PERIOD_READ,
                ENROLLMENT_PERIOD_CREATE,
                ENROLLMENT_PERIOD_UPDATE,
                COURSE_ENROLLMENT_READ);
      }
      case "academic-years" -> {
        entity = "AcademicYear";
        permissions =
            Set.of(
                ACADEMIC_YEAR_READ,
                COURSE_READ,
                COURSE_CREATE,
                COURSE_UPDATE,
                ENROLLMENT_PERIOD_READ,
                ENROLLMENT_PERIOD_CREATE,
                ENROLLMENT_PERIOD_UPDATE,
                COURSE_ENROLLMENT_READ);
      }
      case "academic-spaces" -> {
        entity = "AcademicSpace";
        permissions = Set.of(ACADEMIC_SPACE_READ, STUDY_PLAN_CURRICULUM_UPDATE);
      }
      case "instruments" -> {
        entity = "Instrument";
        permissions = Set.of(INSTRUMENT_READ, COURSE_CREATE, COURSE_UPDATE);
      }
      default -> throw new ScopedResourceNotFoundException();
    }
    if (operation != null) {
      if (!permissions.contains(operation)) {
        throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
      }
      permissions = Set.of(operation);
    }
    PermissionAccess access = PermissionAccess.none();
    for (var permission : permissions) {
      access = access.union(authorization.managementAccess(permission));
    }
    if (!access.institutional() && access.trainingPathIds().isEmpty()) {
      throw new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE);
    }

    var parameters = new HashMap<String, Object>();
    parameters.put("institution", institutionId);
    String where = " WHERE item.institution.id = :institution AND item.deletedAt IS NULL";
    if (pathExpression != null && !access.institutional()) {
      where += " AND " + pathExpression + " IN :paths";
      parameters.put("paths", access.trainingPathIds());
    }
    if (trainingPathId != null && pathExpression != null) {
      where += " AND " + pathExpression + " = :trainingPath";
      parameters.put("trainingPath", trainingPathId);
    }
    if (entity.equals("StudyPlan")) {
      if (status != null) {
        where += " AND item.status = :status";
        parameters.put("status", status);
      }
      if (Boolean.TRUE.equals(published)) {
        where += " AND item.status <> :draft";
        parameters.put("draft", StudyPlanStatus.DRAFT);
      }
    } else if (entity.equals("AcademicYear")) {
      if (active != null) {
        where += active ? " AND item.status = :yearStatus" : " AND item.status <> :yearStatus";
        parameters.put("yearStatus", AcademicYearStatus.ACTIVE);
      }
    } else if (active != null) {
      where += " AND item.active = :active";
      parameters.put("active", active);
    }
    if (search != null && !search.isBlank()) {
      where +=
          entity.equals("AcademicYear")
              ? " AND cast(item.year as string) LIKE :search"
              : " AND lower(item.name) LIKE :search";
      parameters.put("search", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
    }
    String from = " FROM " + entity + " item";
    var countQuery = entityManager.createQuery("SELECT count(item)" + from + where, Long.class);
    var query =
        entityManager.createQuery(
            "SELECT item"
                + from
                + where
                + (entity.equals("AcademicYear")
                    ? " ORDER BY item.year DESC, item.id"
                    : " ORDER BY item.name, item.id"),
            Object.class);
    parameters.forEach(
        (name, value) -> {
          query.setParameter(name, value);
          countQuery.setParameter(name, value);
        });
    long total = countQuery.getSingleResult();
    var items =
        query
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultStream()
            .map(this::option)
            .toList();
    return new PaginatedResponse<>(
        items,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        total,
        (int) Math.ceil((double) total / pageable.getPageSize()));
  }

  private AcademicSelectionOptionResponse option(Object value) {
    if (value instanceof StudyPlan item) {
      return new AcademicSelectionOptionResponse(
          item.getId(),
          item.getName(),
          null,
          item.getTrainingPath().getId(),
          item.getTrainingPath().getName(),
          item.getVersionNumber(),
          null,
          null,
          null);
    }
    if (value instanceof TrainingPath item) {
      return new AcademicSelectionOptionResponse(
          item.getId(), item.getName(), null, item.getId(), item.getName(), null, null, null, null);
    }
    if (value instanceof AcademicYear item) {
      return new AcademicSelectionOptionResponse(
          item.getId(),
          String.valueOf(item.getYear()),
          item.getYear(),
          null,
          null,
          null,
          null,
          null,
          null);
    }
    if (value instanceof AcademicSpace item) {
      return new AcademicSelectionOptionResponse(
          item.getId(),
          item.getName(),
          null,
          null,
          null,
          null,
          item.getType().name(),
          item.getFormat().name(),
          item.isInstrumental());
    }
    var item = (Instrument) value;
    return new AcademicSelectionOptionResponse(
        item.getId(), item.getName(), null, null, null, null, null, null, null);
  }
}
