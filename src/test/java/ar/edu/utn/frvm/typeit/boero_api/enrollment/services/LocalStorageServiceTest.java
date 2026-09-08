package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTest {

  @TempDir Path tempDir;

  private LocalStorageService storageService;
  private final UUID applicationId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    storageService = new LocalStorageService(tempDir.toString());
  }

  @Test
  @DisplayName("Should successfully store a valid PDF file")
  void store_validPdf() throws Exception {
    byte[] content = "test pdf content".getBytes();
    MockMultipartFile file =
        new MockMultipartFile("file", "document.pdf", "application/pdf", content);

    LocalStorageService.StoredFile stored = storageService.store(applicationId, file);

    assertThat(stored).isNotNull();
    assertThat(stored.safeFileName()).endsWith(".pdf");
    assertThat(stored.contentType()).isEqualTo("application/pdf");
    assertThat(stored.size()).isEqualTo(content.length);

    Resource resource = storageService.loadAsResource(stored.storagePath());
    assertThat(resource.exists()).isTrue();
    assertThat(resource.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  @DisplayName("Should successfully store a valid PNG image")
  void store_validPng() throws Exception {
    byte[] content = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", content);

    LocalStorageService.StoredFile stored = storageService.store(applicationId, file);

    assertThat(stored.safeFileName()).endsWith(".png");
    assertThat(stored.contentType()).isEqualTo("image/png");
  }

  @Test
  @DisplayName("Should reject empty file")
  void store_emptyFile() {
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

    assertThatThrownBy(() -> storageService.store(applicationId, file))
        .isInstanceOf(InvalidFileException.class)
        .hasMessageContaining("no puede estar vacío");
  }

  @Test
  @DisplayName("Should reject file exceeding max size")
  void store_fileTooLarge() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            new byte[(int) (LocalStorageService.MAX_FILE_SIZE_BYTES + 1)]);

    assertThatThrownBy(() -> storageService.store(applicationId, file))
        .isInstanceOf(InvalidFileException.class)
        .hasMessageContaining("no puede superar los 10MB");
  }

  @Test
  @DisplayName("Should reject unsupported mime type")
  void store_unsupportedMimeType() {
    MockMultipartFile file =
        new MockMultipartFile("file", "script.sh", "application/x-sh", "echo hi".getBytes());

    assertThatThrownBy(() -> storageService.store(applicationId, file))
        .isInstanceOf(InvalidFileException.class)
        .hasMessageContaining("Tipo de archivo no permitido");
  }

  @Test
  @DisplayName("Should reject path traversal attempts when loading resource")
  void loadAsResource_pathTraversal() {
    assertThatThrownBy(() -> storageService.loadAsResource("../../../etc/passwd"))
        .isInstanceOf(InvalidFileException.class)
        .hasMessageContaining("Acceso no permitido");
  }

  @Test
  @DisplayName("Should throw AttachmentNotFoundException for non-existent file")
  void loadAsResource_notFound() {
    assertThatThrownBy(() -> storageService.loadAsResource(applicationId + "/missing.pdf"))
        .isInstanceOf(AttachmentNotFoundException.class);
  }

  @Test
  @DisplayName("Should read stored file as InputStream")
  void loadAsInputStream_success() throws Exception {
    byte[] content = "sample data".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "sample.jpg", "image/jpeg", content);

    LocalStorageService.StoredFile stored = storageService.store(applicationId, file);

    try (InputStream is = storageService.loadAsInputStream(stored.storagePath())) {
      byte[] readBytes = is.readAllBytes();
      assertThat(readBytes).isEqualTo(content);
    }
  }

  @Test
  @DisplayName("Should delete physical file from storage")
  void deletePhysicalFile_success() {
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

    LocalStorageService.StoredFile stored = storageService.store(applicationId, file);
    Path filePath = tempDir.resolve(stored.storagePath());
    assertThat(Files.exists(filePath)).isTrue();

    boolean deleted = storageService.deletePhysicalFile(stored.storagePath());
    assertThat(deleted).isTrue();
    assertThat(Files.exists(filePath)).isFalse();
  }
}
