package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static java.util.Objects.requireNonNull;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.common.storage.StorageService;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentVersionStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentDocumentAction;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentAttachmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.DocumentReviewRequest;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAttachmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDocumentRequirementResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.services.EnrollmentDocumentAudit.Actor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EnrollmentAttachmentService {
  private final EnrollmentApplicationRepository applicationRepository;
  private final EnrollmentAttachmentRepository attachmentRepository;
  private final StorageService storage;
  private final EnrollmentDocumentAuthorization authorization;
  private final EnrollmentDocumentAudit audit;
  private final EnrollmentStorageJournal journal;
  private final Clock clock;
  private final EnrollmentInstitutionLock institutionLock;
  private final EntityManager entityManager;
  private final EnrollmentDocumentRequirementsService documents;

  public record AttachmentContentResult(Resource resource, EnrollmentAttachment attachment) {}

  @Transactional
  public EnrollmentAttachmentResponse uploadAttachment(
      final UUID applicationId,
      final MultipartFile file,
      final UUID requirementId,
      final @Nullable Authentication authentication) {
    final var application =
        lockApplication(
            applicationId,
            authentication,
            PermissionCode.ENROLLMENT_ATTACHMENT_UPLOAD,
            EnrollmentDocumentAction.UPLOAD,
            null);
    requireEditable(application);

    final var requirement = documents.requirement(application, requirementId);
    documents.requireActive(requirement);
    final var previous =
        attachmentRepository
            .findByEnrollmentApplicationIdAndRequirementIdAndVersionStatusAndDeletedAtIsNull(
                applicationId, requirementId, DocumentVersionStatus.CURRENT);
    if (previous.isPresent()) {
      previous.get().requireMutable();
      authorization.require(
          application,
          authentication,
          PermissionCode.ENROLLMENT_ATTACHMENT_DELETE,
          EnrollmentDocumentAction.REPLACE,
          previous.get().getId());
    }
    final var stored = EnrollmentFilePolicy.prepare(applicationId, file);
    if (!requirement.getAllowedFormats().contains(stored.contentType())) {
      throw new InvalidFileException(EnrollmentMessages.DOCUMENT_FORMAT_INVALID);
    }
    final var actor = Actor.from(authentication);
    final var reservation =
        journal.reserve(application.getInstitution().getId(), applicationId, stored, actor);

    // Keep this row locked until commit: cleanup cannot race an in-flight upload or confirmation.
    journal.lockReservation(reservation);
    storage.write(stored.storagePath(), stored.contentType(), stored.size(), file);

    if (previous.isPresent()) {
      final var existing = previous.get();
      existing.supersede();
      attachmentRepository.saveAndFlush(existing);
      audit.record(
          actor,
          application.getInstitution().getId(),
          applicationId,
          existing.getId(),
          EnrollmentDocumentAction.DELETE,
          "SUCCESS");
    }
    final String originalName =
        file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
            ? stored.safeFileName()
            : file.getOriginalFilename();
    final var attachment =
        EnrollmentAttachment.builder()
            .enrollmentApplication(application)
            .requirement(requirement)
            .uploadedBy(requireNonNull(actor.id()))
            .uploaderType(actor.accountType())
            .originalFileName(originalName)
            .storagePath(stored.storagePath())
            .contentType(stored.contentType())
            .fileSize(stored.size())
            .build();

    try {
      attachmentRepository.saveAndFlush(attachment);
      journal.confirm(reservation, attachment.getId());
      audit.record(
          actor,
          application.getInstitution().getId(),
          applicationId,
          attachment.getId(),
          previous.isPresent() ? EnrollmentDocumentAction.REPLACE : EnrollmentDocumentAction.UPLOAD,
          "SUCCESS");
      return EnrollmentAttachmentResponse.from(attachment);
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "enrollment_attachments_current_requirement_unique"
                .equals(violation.getConstraintName())) {
          throw new EnrollmentValidationException(EnrollmentMessages.ATTACHMENT_TYPE_CONFLICT);
        }
      }
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public AttachmentContentResult getAttachmentContent(
      final UUID applicationId,
      final UUID attachmentId,
      final @Nullable Authentication authentication) {
    final var application = getActiveApplication(applicationId);
    authorization.require(
        application,
        authentication,
        PermissionCode.ENROLLMENT_ATTACHMENT_READ,
        EnrollmentDocumentAction.CONTENT_ACCESS,
        attachmentId);
    final var attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

    // Commit the access event before opening a stream, including when the outer transaction is
    // read-only.
    audit.recordIndependent(
        authentication,
        application.getInstitution().getId(),
        applicationId,
        attachmentId,
        EnrollmentDocumentAction.CONTENT_ACCESS,
        "ALLOWED");
    return new AttachmentContentResult(
        storage.loadAsResource(attachment.getStoragePath()), attachment);
  }

  @Transactional
  public void deleteAttachment(
      final UUID applicationId,
      final UUID attachmentId,
      final @Nullable Authentication authentication) {
    final var application =
        lockApplication(
            applicationId,
            authentication,
            PermissionCode.ENROLLMENT_ATTACHMENT_DELETE,
            EnrollmentDocumentAction.DELETE,
            attachmentId);
    requireEditable(application);
    final var attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
    final var actor = Actor.from(authentication);

    documents.requireActive(attachment.getRequirement());
    attachment.requireMutable();
    if (!application.isEditable()
        && attachment.getRequirement().getOrigin()
            == ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementOrigin.ORIGINAL
        && attachment.getRequirement().getLevel() == DocumentRequirementLevel.AT_SUBMISSION) {
      throw new EnrollmentValidationException(EnrollmentMessages.DOCUMENT_WITHDRAW_DENIED);
    }
    attachment.withdraw();
    attachmentRepository.save(attachment);
    audit.record(
        actor,
        application.getInstitution().getId(),
        applicationId,
        attachmentId,
        EnrollmentDocumentAction.DELETE,
        "SUCCESS");
  }

  @Transactional(readOnly = true)
  public List<EnrollmentAttachmentResponse> listAttachments(
      final UUID applicationId, final @Nullable Authentication authentication) {
    final var application = getActiveApplication(applicationId);
    authorization.require(
        application,
        authentication,
        PermissionCode.ENROLLMENT_ATTACHMENT_READ,
        EnrollmentDocumentAction.METADATA_READ,
        null);
    audit.recordIndependent(
        authentication,
        application.getInstitution().getId(),
        applicationId,
        null,
        EnrollmentDocumentAction.METADATA_READ,
        "ALLOWED");
    return attachmentRepository
        .findByEnrollmentApplicationIdAndDeletedAtIsNull(applicationId)
        .stream()
        .filter(mappedEnrollmentAttachment -> mappedEnrollmentAttachment.isCurrent())
        .map(EnrollmentAttachmentResponse::from)
        .toList();
  }

  private EnrollmentApplication getActiveApplication(final UUID applicationId) {
    return applicationRepository
        .findById(applicationId)
        .filter(app -> app.getDeletedAt() == null)
        .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
  }

  private EnrollmentApplication lockApplication(
      final UUID applicationId,
      final @Nullable Authentication authentication,
      final PermissionCode permission,
      final EnrollmentDocumentAction action,
      final @Nullable UUID attachmentId) {
    final var application = getActiveApplication(applicationId);
    authorization.require(application, authentication, permission, action, attachmentId);
    final var institutionId = application.getInstitution().getId();
    institutionLock.lock(institutionId);
    final var locked =
        applicationRepository
            .findForAttachmentUpdate(applicationId, institutionId)
            .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
    entityManager.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
    authorization.require(locked, authentication, permission, action, attachmentId);
    if (locked.getDeletedAt() != null) {
      throw new EnrollmentApplicationNotFoundException(applicationId);
    }
    return locked;
  }

  private void requireEditable(final EnrollmentApplication application) {
    if (!documents.editable(application)) {
      throw new ApplicationNotEditableException(application.getId());
    }
  }

  @Transactional
  public EnrollmentAttachmentResponse review(
      UUID applicationId,
      UUID attachmentId,
      DocumentReviewRequest request,
      Authentication authentication) {
    var application =
        lockApplication(
            applicationId,
            authentication,
            PermissionCode.ENROLLMENT_ATTACHMENT_REVIEW,
            EnrollmentDocumentAction.REVIEW,
            attachmentId);
    requireEditable(application);
    if (application.isEditable()) {
      throw new EnrollmentValidationException(EnrollmentMessages.DOCUMENT_REVIEW_INVALID);
    }
    var attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
    var actor = Actor.from(authentication);
    documents.requireActive(attachment.getRequirement());
    attachment.review(
        request.status(),
        request.observation(),
        requireNonNull(actor.id()),
        actor.accountType(),
        clock.instant());
    audit.record(
        actor,
        application.getInstitution().getId(),
        applicationId,
        attachmentId,
        EnrollmentDocumentAction.REVIEW,
        "SUCCESS");
    return EnrollmentAttachmentResponse.from(attachment);
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentRequirementResponse> requirements(
      UUID applicationId, Authentication authentication) {
    var application = getActiveApplication(applicationId);
    authorization.require(
        application,
        authentication,
        PermissionCode.ENROLLMENT_ATTACHMENT_READ,
        EnrollmentDocumentAction.METADATA_READ,
        null);
    audit.recordIndependent(
        authentication,
        application.getInstitution().getId(),
        applicationId,
        null,
        EnrollmentDocumentAction.METADATA_READ,
        "ALLOWED");
    return documents.responses(application, authentication);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<EnrollmentAttachmentResponse> history(
      UUID applicationId, UUID requirementId, Pageable pageable, Authentication authentication) {
    var application = getActiveApplication(applicationId);
    authorization.require(
        application,
        authentication,
        PermissionCode.ENROLLMENT_ATTACHMENT_READ,
        EnrollmentDocumentAction.METADATA_READ,
        null);
    documents.requirement(application, requirementId);
    audit.recordIndependent(
        authentication,
        application.getInstitution().getId(),
        applicationId,
        null,
        EnrollmentDocumentAction.METADATA_READ,
        "ALLOWED");
    return PaginatedResponse.from(
        attachmentRepository
            .findByEnrollmentApplicationIdAndRequirementIdAndDeletedAtIsNull(
                applicationId, requirementId, pageable)
            .map(EnrollmentAttachmentResponse::from));
  }
}
