package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalAuthoritySnapshot;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.PermissionAccess;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentAttachmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentStorage;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentStorage.StoredFile;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAttachmentResponse;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentAttachmentService {

  private final EnrollmentApplicationRepository applicationRepository;
  private final EnrollmentAttachmentRepository attachmentRepository;
  private final EnrollmentStorage storage;
  private final AuthorizationService authorizationService;
  private final AuthorityResolver authorityResolver;
  private final Clock clock;
  private final EnrollmentApplicationPeriodService applicationPeriodService;
  private final EnrollmentInstitutionLock institutionLock;

  public record AttachmentContentResult(Resource resource, EnrollmentAttachment attachment) {}

  @Transactional
  public EnrollmentAttachmentResponse uploadAttachment(
      UUID applicationId,
      MultipartFile file,
      String attachmentTypeStr,
      Authentication authentication) {

    EnrollmentAttachmentType attachmentType = parseAttachmentType(attachmentTypeStr);

    EnrollmentApplication application = lockApplication(applicationId, authentication);
    ensureCanModify(application, authentication);
    applicationPeriodService.requireOpen(application);

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    StoredFile storedFile = storage.store(applicationId, file);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              storage.deletePhysicalFile(storedFile.storagePath());
            }
          }
        });

    // 2. Reemplazar adjunto existente del mismo tipo si ya existiera
    attachmentRepository
        .findByEnrollmentApplicationIdAndAttachmentTypeAndDeletedAtIsNull(
            applicationId, attachmentType)
        .ifPresent(
            existing -> {
              existing.markDeleted(clock.instant());
              attachmentRepository.saveAndFlush(existing);
              deleteAfterCommit(existing.getStoragePath());
            });

    // 3. Crear nuevo registro de adjunto
    String originalName =
        (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
            ? file.getOriginalFilename()
            : storedFile.safeFileName();

    EnrollmentAttachment attachment =
        EnrollmentAttachment.builder()
            .enrollmentApplication(application)
            .attachmentType(attachmentType)
            .originalFileName(originalName)
            .storagePath(storedFile.storagePath())
            .contentType(storedFile.contentType())
            .fileSize(storedFile.size())
            .build();

    try {
      return EnrollmentAttachmentResponse.from(attachmentRepository.saveAndFlush(attachment));
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "enrollment_attachments_active_type_unique".equals(violation.getConstraintName())) {
          throw new EnrollmentValidationException(EnrollmentMessages.ATTACHMENT_TYPE_CONFLICT);
        }
      }

      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public AttachmentContentResult getAttachmentContent(
      UUID applicationId, UUID attachmentId, Authentication authentication) {

    EnrollmentApplication application = getActiveApplication(applicationId);
    ensureCanAccess(application, authentication);

    EnrollmentAttachment attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

    Resource resource = storage.loadAsResource(attachment.getStoragePath());

    return new AttachmentContentResult(resource, attachment);
  }

  @Transactional
  public void deleteAttachment(
      UUID applicationId, UUID attachmentId, Authentication authentication) {

    EnrollmentApplication application = lockApplication(applicationId, authentication);
    ensureCanModify(application, authentication);
    applicationPeriodService.requireOpen(application);

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    EnrollmentAttachment attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

    attachment.markDeleted(clock.instant());
    attachmentRepository.save(attachment);
    deleteAfterCommit(attachment.getStoragePath());
  }

  @Transactional(readOnly = true)
  public List<EnrollmentAttachmentResponse> listAttachments(
      UUID applicationId, Authentication authentication) {

    EnrollmentApplication application = getActiveApplication(applicationId);
    ensureCanAccess(application, authentication);

    return attachmentRepository
        .findByEnrollmentApplicationIdAndDeletedAtIsNull(applicationId)
        .stream()
        .map(EnrollmentAttachmentResponse::from)
        .toList();
  }

  private EnrollmentApplication getActiveApplication(UUID applicationId) {
    return applicationRepository
        .findById(applicationId)
        .filter(app -> app.getDeletedAt() == null)
        .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
  }

  private EnrollmentApplication lockApplication(UUID applicationId, Authentication authentication) {
    if (authentication == null) {
      throw new AccessDeniedException(EnrollmentMessages.ATTACHMENT_ACCESS_DENIED);
    }

    UUID institutionId;

    if (authentication.getPrincipal() instanceof JwtAuthenticatedUser user) {
      if (user.institutionId() == null) {
        throw new AccessDeniedException(EnrollmentMessages.ATTACHMENT_ACCESS_DENIED);
      }

      institutionId = user.institutionId();
    } else if (authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount
        && authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN)) {
      institutionId = null;
    } else {
      throw new AccessDeniedException(EnrollmentMessages.ATTACHMENT_ACCESS_DENIED);
    }

    final UUID resolvedInstitutionId =
        institutionId == null
            ? applicationRepository
                .findById(applicationId)
                .map(app -> app.getInstitution().getId())
                .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId))
            : institutionId;
    institutionLock.lock(resolvedInstitutionId);

    return applicationRepository
        .findForAttachmentUpdate(applicationId, institutionId)
        .orElseThrow(() -> new EnrollmentApplicationNotFoundException(applicationId));
  }

  private void deleteAfterCommit(String storagePath) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            storage.deletePhysicalFile(storagePath);
          }
        });
  }

  private EnrollmentAttachmentType parseAttachmentType(String typeStr) {
    if (typeStr == null || typeStr.isBlank()) {
      throw new InvalidFileException(EnrollmentMessages.ATTACHMENT_TYPE_REQUIRED);
    }

    try {
      return EnrollmentAttachmentType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new InvalidFileException(
          EnrollmentMessages.ATTACHMENT_TYPE_INVALID
              + Arrays.toString(EnrollmentAttachmentType.values()));
    }
  }

  private void ensureCanAccess(EnrollmentApplication application, Authentication authentication) {
    if (isAuthorized(application, authentication)) {
      return;
    }

    denyAttachmentAccess(application, authentication);
  }

  private void denyAttachmentAccess(
      EnrollmentApplication application, Authentication authentication) {
    if (authentication != null
        && authentication.getPrincipal() instanceof JwtAuthenticatedUser user
        && authorityResolver
            .resolvePersonAuthorities(user.personId(), user.institutionId())
            .permissions()
            .contains(PermissionCode.ENROLLMENT_APPLICATION_READ)) {
      throw new EnrollmentApplicationNotFoundException(application.getId());
    }
    throw new AccessDeniedException(EnrollmentMessages.ATTACHMENT_ACCESS_DENIED);
  }

  private void ensureCanModify(EnrollmentApplication application, Authentication authentication) {
    if (isAuthorized(application, authentication)) {
      return;
    }

    denyAttachmentAccess(application, authentication);
  }

  private boolean isAuthorized(EnrollmentApplication application, Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      return false;
    }

    if (authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount) {
      return authorizationService.hasPlatformRole(authentication, PlatformRoleCode.PLATFORM_ADMIN);
    }

    if (authentication.getPrincipal() instanceof JwtAuthenticatedUser user) {
      UUID applicantPersonId = application.getApplicantPerson().getId();

      if (user.personId() != null && user.personId().equals(applicantPersonId)) {
        return true;
      }

      if (application.getInstitution() != null
          && user.institutionId().equals(application.getInstitution().getId())) {
        InstitutionalAuthoritySnapshot snapshot =
            authorityResolver.resolvePersonAuthorities(
                user.personId(), application.getInstitution().getId());

        return snapshot
            .permissionScopes()
            .getOrDefault(PermissionCode.ENROLLMENT_APPLICATION_READ, PermissionAccess.none())
            .includes(application.getTrainingPathId());
      }
    }

    return false;
  }
}
