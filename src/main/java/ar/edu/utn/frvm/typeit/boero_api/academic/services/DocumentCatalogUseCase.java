package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.*;
import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.*;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.*;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.*;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.*;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.ScopedResourceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.*;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentInstitutionLock;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.SynchronizeDraftDocumentsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentCatalogUseCase {
  private final EnrollmentApplicationRepository applications;
  private final DocumentDefinitionRepository definitions;
  private final TrainingPathDocumentRequirementRepository assignments;
  private final TrainingPathRepository paths;
  private final InstitutionRepository institutions;
  private final ScopedAuthorizationService authorization;
  private final AcademicAccessGuard access;
  private final EnrollmentInstitutionLock lock;
  private final SynchronizeDraftDocumentsUseCase synchronize;

  private void require(final PermissionCode permission, final UUID institutionId) {
    authorization.require(permission, institutionId, new UUID(0, 0));
  }

  @Transactional(readOnly = true)
  public Page<DocumentDefinitionResponse> list(
      final UUID institutionId,
      final String search,
      final @Nullable Boolean active,
      final @Nullable UUID pathId,
      final @Nullable UUID applicationId,
      final boolean forTrainingPathCreation,
      final Pageable pageable) {
    if (applicationId != null) {
      var app =
          applications
              .findById(applicationId)
              .filter(
                  value ->
                      value.getDeletedAt() == null
                          && value.getInstitution().getId().equals(institutionId))
              .orElseThrow(
                  () ->
                      new DocumentCatalogException(
                          ErrorCategory.NOT_FOUND, AcademicMessages.DOCUMENT_NOT_FOUND));
      authorization.require(
          PermissionCode.ENROLLMENT_DOCUMENT_REQUEST_CREATE,
          institutionId,
          app.getTrainingPathId());
    } else if (pathId == null && forTrainingPathCreation) {
      require(PermissionCode.TRAINING_PATH_CREATE, institutionId);
    } else if (pathId == null) {
      require(PermissionCode.DOCUMENT_CATALOG_READ, institutionId);
    } else {
      access.require(
          PermissionCode.TRAINING_PATH_READ, institutionId, ScopedResource.TRAINING_PATH, pathId);
    }
    return definitions
        .findAll(filters(institutionId, search, active), pageable)
        .map(DocumentDefinitionResponse::from);
  }

  @Transactional(readOnly = true)
  public Page<PlatformDocumentDefinitionResponse> listPlatform(
      final @Nullable UUID institutionId,
      final String search,
      final @Nullable Boolean active,
      final Pageable pageable) {
    if (!authorization.isPlatformAdministrator()) {
      throw new ScopedResourceNotFoundException();
    }

    return definitions
        .findAll(filters(institutionId, search, active), pageable)
        .map(PlatformDocumentDefinitionResponse::from);
  }

  private Specification<DocumentDefinition> filters(
      final @Nullable UUID institutionId, final String search, final @Nullable Boolean active) {
    return (root, query, cb) -> {
      var predicate = cb.conjunction();
      if (institutionId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("institution").get("id"), institutionId));
      }
      if (active != null) {
        predicate = cb.and(predicate, cb.equal(root.get("active"), active));
      }
      if (!search.isBlank()) {
        String text = SearchNormalization.escapeLike(search.trim().toLowerCase(Locale.ROOT));
        predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + text + "%", '\\'));
      }
      return predicate;
    };
  }

  @Transactional(readOnly = true)
  public DocumentDefinitionResponse get(final UUID institutionId, final UUID id) {
    require(PermissionCode.DOCUMENT_CATALOG_READ, institutionId);
    var value = definition(institutionId, id);
    long pathCount = assignments.countByDocumentId(id);
    long draftCount = applications.countDraftsUsingDocument(id);
    return DocumentDefinitionResponse.from(value, canChangeInstitution(value, pathCount))
        .withImpact(pathCount, draftCount);
  }

  private boolean canChangeInstitution(final DocumentDefinition value, final long assignmentCount) {
    return authorization.isPlatformAdministrator()
        && value.getUsedAt() == null
        && assignmentCount == 0
        && !definitions.hasEnrollmentUsage(value.getId());
  }

  private DocumentDefinition definition(final UUID institutionId, final UUID id) {
    return definitions
        .findByIdAndInstitutionId(id, institutionId)
        .orElseThrow(
            () ->
                new DocumentCatalogException(
                    ErrorCategory.NOT_FOUND, AcademicMessages.DOCUMENT_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public Page<DocumentRequirementResponse> associations(
      final UUID institutionId,
      final UUID id,
      final @Nullable UUID pathId,
      final @Nullable Boolean active,
      final Pageable pageable) {
    require(PermissionCode.DOCUMENT_CATALOG_READ, institutionId);
    definition(institutionId, id);
    final var readAccess = authorization.managementAccess(PermissionCode.TRAINING_PATH_READ);
    final var readablePaths =
        readAccess.trainingPathIds().isEmpty()
            ? Set.of(new UUID(0, 0))
            : readAccess.trainingPathIds();

    return assignments
        .findAll(
            (root, query, cb) ->
                cb.and(
                    cb.equal(root.get("document").get("id"), id),
                    cb.equal(root.get("institutionId"), institutionId),
                    cb.isNull(root.get("trainingPath").get("deletedAt")),
                    active == null ? cb.conjunction() : cb.equal(root.get("active"), active),
                    pathId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("trainingPath").get("id"), pathId),
                    readAccess.institutional()
                        ? cb.conjunction()
                        : root.get("trainingPath").get("id").in(readablePaths)),
            pageable)
        .map(DocumentRequirementResponse::from);
  }

  @Transactional
  public DocumentCatalogSaveResponse save(
      final UUID institutionId, final @Nullable UUID id, final DocumentDefinitionRequest request) {
    require(PermissionCode.DOCUMENT_CATALOG_MANAGE, institutionId);
    final UUID destinationId =
        request.targetInstitutionId() == null ? institutionId : request.targetInstitutionId();
    final boolean transferring = !destinationId.equals(institutionId);
    if (transferring && (id == null || !authorization.isPlatformAdministrator())) {
      throw new ScopedResourceNotFoundException();
    }

    for (UUID tenantId : new TreeSet<>(List.of(institutionId, destinationId))) {
      lock.lock(tenantId);
    }
    var value =
        id == null
            ? DocumentDefinition.create(
                institutions
                    .findById(institutionId)
                    .orElseThrow(
                        () ->
                            new DocumentCatalogException(
                                ErrorCategory.NOT_FOUND, AcademicMessages.DOCUMENT_NOT_FOUND)))
            : definition(institutionId, id);
    if (id != null) {
      revision(value.getRevision(), request.revision());
    }
    if (transferring) {
      if (value.getUsedAt() != null
          || assignments.countByDocumentId(value.getId()) > 0
          || definitions.hasEnrollmentUsage(value.getId())) {
        throw new DocumentCatalogException(
            ErrorCategory.CONFLICT, AcademicMessages.DOCUMENT_INSTITUTION_CHANGE_BLOCKED);
      }
      value.changeInstitution(institutions.findById(destinationId).orElseThrow());
    }

    var changes =
        request.assignments() == null
            ? List.<DocumentAssignmentRequest>of()
            : request.assignments();
    validateAssignments(destinationId, value, changes);
    value.update(
        request.name(), request.instructions(), request.allowedFormats(), request.active());
    definitions.saveAndFlush(value);
    Set<UUID> affected = new HashSet<>();
    for (var assignment : assignments.findByDocumentId(value.getId())) {
      affected.add(assignment.getTrainingPath().getId());
    }
    for (var change : changes) {
      apply(destinationId, value, change);
      affected.add(change.trainingPathId());
    }
    assignments.flush();
    int drafts = synchronize.execute(destinationId, affected);
    return result(destinationId, value.getId(), affected.size(), drafts);
  }

  @Transactional
  public DocumentCatalogSaveResponse saveAssignments(
      final UUID institutionId, final UUID id, final List<DocumentAssignmentRequest> changes) {
    if (changes.isEmpty()) {
      require(PermissionCode.DOCUMENT_CATALOG_READ, institutionId);
    }
    lock.lock(institutionId);
    var value = definition(institutionId, id);
    validateAssignments(institutionId, value, changes);
    Set<UUID> affected = new HashSet<>();
    for (var change : changes) {
      apply(institutionId, value, change);
      affected.add(change.trainingPathId());
    }
    assignments.flush();
    int drafts = synchronize.execute(institutionId, affected);
    return result(institutionId, id, affected.size(), drafts);
  }

  @Transactional
  public DocumentRequirementResponse saveAssignment(
      final UUID institutionId,
      final UUID pathId,
      final @Nullable UUID id,
      final DocumentRequirementRequest request) {
    access.require(
        PermissionCode.TRAINING_PATH_UPDATE, institutionId, ScopedResource.TRAINING_PATH, pathId);
    lock.lock(institutionId);
    var document = definition(institutionId, request.documentId());
    if (id != null) {
      var existing =
          assignments
              .findByIdAndTrainingPathId(id, pathId)
              .orElseThrow(
                  () ->
                      new DocumentCatalogException(
                          ErrorCategory.NOT_FOUND, AcademicMessages.DOCUMENT_NOT_FOUND));
      if (!existing.getDocument().getId().equals(request.documentId())) {
        throw new DocumentCatalogException(
            ErrorCategory.CONFLICT, AcademicMessages.DOCUMENT_ASSIGNMENT_IMMUTABLE);
      }
    }
    var change =
        new DocumentAssignmentRequest(
            pathId,
            request.revision(),
            request.level(),
            request.displayOrder(),
            request.active(),
            request.specificInstructions());
    validateAssignments(institutionId, document, List.of(change));
    var saved = apply(institutionId, document, change);
    assignments.flush();
    UUID savedId = saved.getId();
    synchronize.execute(institutionId, Set.of(pathId));
    return DocumentRequirementResponse.from(assignments.findById(savedId).orElseThrow());
  }

  private void validateAssignments(
      final UUID institutionId,
      final DocumentDefinition document,
      final List<DocumentAssignmentRequest> changes) {
    Set<UUID> seen = new HashSet<>();
    for (var change : changes) {
      if (!seen.add(change.trainingPathId())) {
        throw new DocumentCatalogException(
            ErrorCategory.CONFLICT, AcademicMessages.DOCUMENT_DUPLICATE_ASSIGNMENT);
      }
      access.require(
          PermissionCode.TRAINING_PATH_UPDATE,
          institutionId,
          ScopedResource.TRAINING_PATH,
          change.trainingPathId());
      path(institutionId, change.trainingPathId());
      if (document.getId() != null) {
        assignments
            .findByTrainingPathIdAndDocumentId(change.trainingPathId(), document.getId())
            .ifPresent(existing -> revision(existing.getRevision(), change.revision()));
      }
    }
  }

  private TrainingPath path(final UUID institutionId, final UUID pathId) {
    return paths
        .findById(pathId)
        .filter(
            value ->
                value.getDeletedAt() == null
                    && value.getInstitution().getId().equals(institutionId))
        .orElseThrow(
            () ->
                new DocumentCatalogException(
                    ErrorCategory.NOT_FOUND, AcademicMessages.DOCUMENT_NOT_FOUND));
  }

  private TrainingPathDocumentRequirement apply(
      final UUID institutionId,
      final DocumentDefinition document,
      final DocumentAssignmentRequest change) {
    var value =
        assignments
            .findByTrainingPathIdAndDocumentId(change.trainingPathId(), document.getId())
            .orElseGet(
                () ->
                    TrainingPathDocumentRequirement.create(
                        path(institutionId, change.trainingPathId()), document));
    value.update(
        change.level(), change.displayOrder(), change.active(), change.specificInstructions());
    return assignments.save(value);
  }

  private void revision(final long current, final @Nullable Long expected) {
    if (expected == null || current != expected.longValue()) {
      throw new DocumentCatalogException(
          ErrorCategory.CONFLICT, AcademicMessages.DOCUMENT_REVISION_CONFLICT);
    }
  }

  private DocumentCatalogSaveResponse result(
      final UUID institutionId, final UUID id, final int pathCount, final int draftCount) {
    final var readAccess = authorization.managementAccess(PermissionCode.TRAINING_PATH_READ);
    var allAssignments = assignments.findByDocumentId(id);
    var value = definition(institutionId, id);
    var visible =
        allAssignments.stream()
            .filter(assignment -> readAccess.includes(assignment.getTrainingPath().getId()))
            .map(DocumentRequirementResponse::from)
            .toList();
    return new DocumentCatalogSaveResponse(
        DocumentDefinitionResponse.from(value, canChangeInstitution(value, allAssignments.size())),
        visible,
        pathCount,
        draftCount);
  }
}
