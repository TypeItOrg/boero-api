package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorizationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalAuthoritySnapshot;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentApplicationRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentAttachmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAttachmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrollmentAttachmentServiceTest {

  @Mock private EnrollmentApplicationRepository applicationRepository;
  @Mock private EnrollmentAttachmentRepository attachmentRepository;
  @Mock private LocalStorageService localStorageService;
  @Mock private AuthorizationService authorizationService;
  @Mock private AuthorityResolver authorityResolver;

  private EnrollmentAttachmentService service;

  private final UUID institutionId = UUID.randomUUID();
  private final UUID applicantPersonId = UUID.randomUUID();
  private final UUID applicationId = UUID.randomUUID();
  private final UUID attachmentId = UUID.randomUUID();

  private EnrollmentApplication application;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();
    service =
        new EnrollmentAttachmentService(
            applicationRepository,
            attachmentRepository,
            localStorageService,
            authorizationService,
            authorityResolver,
            Clock.systemUTC(),
            Mockito.mock(EnrollmentApplicationPeriodService.class),
            Mockito.mock(EnrollmentInstitutionLock.class));

    Institution institution = Mockito.mock(Institution.class);
    Mockito.lenient().when(institution.getId()).thenReturn(institutionId);

    Person applicant = Mockito.mock(Person.class);
    Mockito.lenient().when(applicant.getId()).thenReturn(applicantPersonId);

    application =
        EnrollmentApplication.builder()
            .id(applicationId)
            .institution(institution)
            .applicantPerson(applicant)
            .status(EnrollmentApplicationStatus.DRAFT)
            .build();
  }

  @AfterEach
  void tearDownTransaction() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private TestingAuthenticationToken authenticationFor(UUID personId) {
    JwtAuthenticatedUser principal =
        JwtAuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .personId(personId)
            .documentNumber("12345678")
            .institutionId(institutionId)
            .sessionId(UUID.randomUUID())
            .tokenId("jti")
            .build();
    return new TestingAuthenticationToken(principal, null);
  }

  @Test
  @DisplayName("Should successfully upload attachment for draft application")
  void uploadAttachment_success() {
    when(applicationRepository.findForAttachmentUpdate(applicationId, institutionId))
        .thenReturn(Optional.of(application));
    when(attachmentRepository.findByEnrollmentApplicationIdAndAttachmentTypeAndDeletedAtIsNull(
            applicationId, EnrollmentAttachmentType.DNI_FRONT))
        .thenReturn(Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());
    LocalStorageService.StoredFile storedFile =
        new LocalStorageService.StoredFile(
            "uuid.pdf", applicationId + "/uuid.pdf", "application/pdf", 7L);
    when(localStorageService.store(applicationId, file)).thenReturn(storedFile);

    EnrollmentAttachment savedAttachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.DNI_FRONT)
            .originalFileName("dni.pdf")
            .storagePath(storedFile.storagePath())
            .contentType("application/pdf")
            .fileSize(7L)
            .build();

    when(attachmentRepository.saveAndFlush(any(EnrollmentAttachment.class)))
        .thenReturn(savedAttachment);

    EnrollmentAttachmentResponse response =
        service.uploadAttachment(
            applicationId, file, "DNI_FRONT", authenticationFor(applicantPersonId));

    assertThat(response.id()).isEqualTo(attachmentId);
    assertThat(response.attachmentType()).isEqualTo(EnrollmentAttachmentType.DNI_FRONT);
    assertThat(response.originalFileName()).isEqualTo("dni.pdf");
    verify(attachmentRepository).saveAndFlush(any(EnrollmentAttachment.class));
  }

  @Test
  @DisplayName("Should replace existing active attachment of the same type")
  void uploadAttachment_replaceExisting() {
    when(applicationRepository.findForAttachmentUpdate(applicationId, institutionId))
        .thenReturn(Optional.of(application));

    EnrollmentAttachment existing =
        EnrollmentAttachment.builder()
            .id(UUID.randomUUID())
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.DNI_FRONT)
            .originalFileName("old_dni.pdf")
            .storagePath("old_path.pdf")
            .contentType("application/pdf")
            .fileSize(10L)
            .build();

    when(attachmentRepository.findByEnrollmentApplicationIdAndAttachmentTypeAndDeletedAtIsNull(
            applicationId, EnrollmentAttachmentType.DNI_FRONT))
        .thenReturn(Optional.of(existing));

    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());
    LocalStorageService.StoredFile storedFile =
        new LocalStorageService.StoredFile(
            "uuid.pdf", applicationId + "/uuid.pdf", "application/pdf", 7L);
    when(localStorageService.store(applicationId, file)).thenReturn(storedFile);

    EnrollmentAttachment savedAttachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.DNI_FRONT)
            .originalFileName("dni.pdf")
            .storagePath(storedFile.storagePath())
            .contentType("application/pdf")
            .fileSize(7L)
            .build();
    when(attachmentRepository.saveAndFlush(any(EnrollmentAttachment.class)))
        .thenReturn(savedAttachment);

    service.uploadAttachment(
        applicationId, file, "DNI_FRONT", authenticationFor(applicantPersonId));

    assertThat(existing.isDeleted()).isTrue();
    verify(localStorageService, never()).deletePhysicalFile("old_path.pdf");
    TransactionSynchronizationUtils.triggerAfterCommit();
    verify(localStorageService).deletePhysicalFile("old_path.pdf");
  }

  @Test
  @DisplayName("Should reject upload when application is not editable")
  void uploadAttachment_notEditable() {
    application.submit();
    when(applicationRepository.findForAttachmentUpdate(applicationId, institutionId))
        .thenReturn(Optional.of(application));

    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());

    assertThatThrownBy(
            () ->
                service.uploadAttachment(
                    applicationId, file, "DNI_FRONT", authenticationFor(applicantPersonId)))
        .isInstanceOf(ApplicationNotEditableException.class);

    verify(localStorageService, never()).store(any(), any());
  }

  @Test
  @DisplayName("Should reject upload when caller is not owner and not administrative")
  void uploadAttachment_unauthorized() {
    when(applicationRepository.findForAttachmentUpdate(applicationId, institutionId))
        .thenReturn(Optional.of(application));
    UUID strangerId = UUID.randomUUID();
    when(authorityResolver.resolvePersonAuthorities(strangerId, institutionId))
        .thenReturn(new InstitutionalAuthoritySnapshot(Set.of(), List.of()));

    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());

    assertThatThrownBy(
            () ->
                service.uploadAttachment(
                    applicationId, file, "DNI_FRONT", authenticationFor(strangerId)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject upload when caller is not authenticated")
  void uploadAttachment_unauthenticated() {

    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());

    assertThatThrownBy(() -> service.uploadAttachment(applicationId, file, "DNI_FRONT", null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject upload with invalid attachment type")
  void uploadAttachment_invalidType() {
    MockMultipartFile file =
        new MockMultipartFile("file", "dni.pdf", "application/pdf", "content".getBytes());

    assertThatThrownBy(
            () ->
                service.uploadAttachment(
                    applicationId, file, "INVALID_TYPE", authenticationFor(applicantPersonId)))
        .isInstanceOf(InvalidFileException.class);
  }

  @Test
  @DisplayName("Should retrieve attachment content for authorized applicant")
  void getAttachmentContent_success() {
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    EnrollmentAttachment attachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.PHOTO_ID)
            .originalFileName("foto.png")
            .storagePath(applicationId + "/foto.png")
            .contentType("image/png")
            .fileSize(100L)
            .build();

    when(attachmentRepository.findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(
            attachmentId, applicationId))
        .thenReturn(Optional.of(attachment));

    Resource mockResource = new ByteArrayResource("image data".getBytes());
    when(localStorageService.loadAsResource(attachment.getStoragePath())).thenReturn(mockResource);

    EnrollmentAttachmentService.AttachmentContentResult result =
        service.getAttachmentContent(
            applicationId, attachmentId, authenticationFor(applicantPersonId));

    assertThat(result.resource()).isEqualTo(mockResource);
    assertThat(result.attachment()).isEqualTo(attachment);
  }

  @Test
  @DisplayName("Should retrieve attachment content for administrative user")
  void getAttachmentContent_adminSuccess() {
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    UUID adminId = UUID.randomUUID();
    when(authorityResolver.resolvePersonAuthorities(adminId, institutionId))
        .thenReturn(new InstitutionalAuthoritySnapshot(Set.of(), List.of("ADMINISTRATIVE")));

    EnrollmentAttachment attachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.PHOTO_ID)
            .originalFileName("foto.png")
            .storagePath(applicationId + "/foto.png")
            .contentType("image/png")
            .fileSize(100L)
            .build();

    when(attachmentRepository.findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(
            attachmentId, applicationId))
        .thenReturn(Optional.of(attachment));

    Resource mockResource = new ByteArrayResource("image data".getBytes());
    when(localStorageService.loadAsResource(attachment.getStoragePath())).thenReturn(mockResource);

    EnrollmentAttachmentService.AttachmentContentResult result =
        service.getAttachmentContent(applicationId, attachmentId, authenticationFor(adminId));

    assertThat(result.resource()).isEqualTo(mockResource);
  }

  @Test
  @DisplayName("Should throw AttachmentNotFoundException when attachment does not exist")
  void getAttachmentContent_notFound() {
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(attachmentRepository.findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(
            attachmentId, applicationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.getAttachmentContent(
                    applicationId, attachmentId, authenticationFor(applicantPersonId)))
        .isInstanceOf(AttachmentNotFoundException.class);
  }

  @Test
  @DisplayName("Should successfully soft delete attachment and remove physical file")
  void deleteAttachment_success() {
    when(applicationRepository.findForAttachmentUpdate(applicationId, institutionId))
        .thenReturn(Optional.of(application));

    EnrollmentAttachment attachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.SECONDARY_CERTIFICATE)
            .originalFileName("cert.pdf")
            .storagePath(applicationId + "/cert.pdf")
            .contentType("application/pdf")
            .fileSize(500L)
            .build();

    when(attachmentRepository.findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(
            attachmentId, applicationId))
        .thenReturn(Optional.of(attachment));

    service.deleteAttachment(applicationId, attachmentId, authenticationFor(applicantPersonId));

    assertThat(attachment.isDeleted()).isTrue();
    verify(attachmentRepository).save(attachment);
    verify(localStorageService, never()).deletePhysicalFile(attachment.getStoragePath());
    TransactionSynchronizationUtils.triggerAfterCommit();
    verify(localStorageService).deletePhysicalFile(attachment.getStoragePath());
  }

  @Test
  @DisplayName("Should list active attachments for application")
  void listAttachments_success() {
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    EnrollmentAttachment attachment =
        EnrollmentAttachment.builder()
            .id(attachmentId)
            .enrollmentApplication(application)
            .attachmentType(EnrollmentAttachmentType.DNI_FRONT)
            .originalFileName("dni.pdf")
            .storagePath(applicationId + "/dni.pdf")
            .contentType("application/pdf")
            .fileSize(200L)
            .build();

    when(attachmentRepository.findByEnrollmentApplicationIdAndDeletedAtIsNull(applicationId))
        .thenReturn(List.of(attachment));

    List<EnrollmentAttachmentResponse> result =
        service.listAttachments(applicationId, authenticationFor(applicantPersonId));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(attachmentId);
  }
}
