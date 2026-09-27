package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPathDocumentRequirement;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathDocumentRequirementRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.DocumentRequirementRequest;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.DocumentRequirementResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.ScopedResource;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AcademicAccessGuard;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentInstitutionLock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingPathDocumentRequirementService {
  private final TrainingPathRepository paths;
  private final TrainingPathDocumentRequirementRepository requirements;
  private final AcademicAccessGuard access;
  private final EnrollmentInstitutionLock lock;

  @Transactional(readOnly = true)
  public List<DocumentRequirementResponse> list(UUID institutionId, UUID pathId) {
    access.require(
        PermissionCode.TRAINING_PATH_READ, institutionId, ScopedResource.TRAINING_PATH, pathId);
    path(institutionId, pathId);
    return requirements.findByTrainingPathIdOrderByDisplayOrderAscIdAsc(pathId).stream()
        .map(DocumentRequirementResponse::from)
        .toList();
  }

  @Transactional
  public DocumentRequirementResponse save(
      UUID institutionId, UUID pathId, @Nullable UUID id, DocumentRequirementRequest request) {
    access.require(
        PermissionCode.TRAINING_PATH_UPDATE, institutionId, ScopedResource.TRAINING_PATH, pathId);
    lock.lock(institutionId);
    var path = path(institutionId, pathId);
    var value =
        id == null
            ? TrainingPathDocumentRequirement.create(path)
            : requirements
                .findByIdAndTrainingPathId(id, pathId)
                .orElseThrow(
                    () ->
                        new EnrollmentValidationException(
                            EnrollmentMessages.DOCUMENT_REQUIREMENT_NOT_FOUND));
    value.update(
        request.name(),
        request.instructions(),
        request.level(),
        request.allowedFormats(),
        request.displayOrder(),
        request.active());
    return DocumentRequirementResponse.from(requirements.saveAndFlush(value));
  }

  private TrainingPath path(UUID institutionId, UUID pathId) {
    return paths
        .findById(pathId)
        .filter(
            value ->
                value.getDeletedAt() == null
                    && value.getInstitution().getId().equals(institutionId))
        .orElseThrow(
            () ->
                new EnrollmentValidationException(
                    EnrollmentMessages.ENROLLMENT_APPLICATION_TRAINING_PATH_INVALID));
  }
}
