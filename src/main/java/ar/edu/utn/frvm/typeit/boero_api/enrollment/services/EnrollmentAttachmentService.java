package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalAuthoritySnapshot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAttachmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories.EnrollmentAttachmentRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentAttachmentService {

  private final EnrollmentApplicationRepository applicationRepository;
  private final EnrollmentAttachmentRepository attachmentRepository;
  private final LocalStorageService localStorageService;
  private final AuthorizationService authorizationService;
  private final AuthorityResolver authorityResolver;

  public record AttachmentContentResult(Resource resource, EnrollmentAttachment attachment) {}

  @Transactional
  public EnrollmentAttachmentResponse uploadAttachment(
      UUID applicationId,
      MultipartFile file,
      String attachmentTypeStr,
      Authentication authentication) {

    EnrollmentAttachmentType attachmentType = parseAttachmentType(attachmentTypeStr);

    EnrollmentApplication application = getActiveApplication(applicationId);
    ensureCanModify(application, authentication);

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    // 1. Guardar archivo físico en disco
    LocalStorageService.StoredFile storedFile = localStorageService.store(applicationId, file);

    // 2. Reemplazar adjunto existente del mismo tipo si ya existiera
    attachmentRepository
        .findByEnrollmentApplicationIdAndAttachmentTypeAndDeletedAtIsNull(
            applicationId, attachmentType)
        .ifPresent(
            existing -> {
              existing.markDeleted();
              attachmentRepository.save(existing);
              localStorageService.deletePhysicalFile(existing.getStoragePath());
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

    EnrollmentAttachment saved = attachmentRepository.save(attachment);
    return EnrollmentAttachmentResponse.from(saved);
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

    Resource resource = localStorageService.loadAsResource(attachment.getStoragePath());
    return new AttachmentContentResult(resource, attachment);
  }

  @Transactional
  public void deleteAttachment(
      UUID applicationId, UUID attachmentId, Authentication authentication) {

    EnrollmentApplication application = getActiveApplication(applicationId);
    ensureCanModify(application, authentication);

    if (application.getStatus() != EnrollmentApplicationStatus.DRAFT) {
      throw new ApplicationNotEditableException(applicationId);
    }

    EnrollmentAttachment attachment =
        attachmentRepository
            .findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(attachmentId, applicationId)
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

    attachment.markDeleted();
    attachmentRepository.save(attachment);
    localStorageService.deletePhysicalFile(attachment.getStoragePath());
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

  private EnrollmentAttachmentType parseAttachmentType(String typeStr) {
    if (typeStr == null || typeStr.isBlank()) {
      throw new InvalidFileException("El tipo de adjunto es obligatorio.");
    }
    try {
      return EnrollmentAttachmentType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new InvalidFileException(
          "Tipo de adjunto no válido. Tipos aceptados: "
              + Arrays.toString(EnrollmentAttachmentType.values()));
    }
  }

  private void ensureCanAccess(EnrollmentApplication application, Authentication authentication) {
    if (isAuthorized(application, authentication)) {
      return;
    }
    throw new AccessDeniedException(
        "No tiene permisos para acceder a los adjuntos de esta solicitud.");
  }

  private void ensureCanModify(EnrollmentApplication application, Authentication authentication) {
    if (isAuthorized(application, authentication)) {
      return;
    }
    throw new AccessDeniedException("No tiene permisos para modificar adjuntos de esta solicitud.");
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
      if (application.getInstitution() != null) {
        InstitutionalAuthoritySnapshot snapshot =
            authorityResolver.resolvePersonAuthorities(
                user.personId(), application.getInstitution().getId());
        return isAdministrativeRole(snapshot);
      }
    }

    return false;
  }

  private boolean isAdministrativeRole(InstitutionalAuthoritySnapshot snapshot) {
    if (snapshot == null) {
      return false;
    }
    return snapshot.roles().contains("ADMINISTRATIVE")
        || snapshot.roles().contains("INSTITUTIONAL_AUTHORITY")
        || snapshot.roles().contains(SystemRoleCode.ADMINISTRATIVE.name())
        || snapshot.roles().contains(SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());
  }
}
