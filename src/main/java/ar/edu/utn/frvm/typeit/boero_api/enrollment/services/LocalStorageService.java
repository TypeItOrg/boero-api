package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@ConditionalOnProperty(
    name = "app.storage.enrollment.provider",
    havingValue = "local",
    matchIfMissing = true)
public class LocalStorageService implements EnrollmentStorage {

  public static final long MAX_FILE_SIZE_BYTES = EnrollmentFilePolicy.MAX_FILE_SIZE_BYTES;

  private final Path baseDir;

  public LocalStorageService(
      @Value("${app.storage.enrollment.base-dir:storage/enrollments}") String baseDirStr) {
    this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.baseDir);
      // Exercise the actual filesystem permissions, including read-only mounts.
      Path probe = Files.createTempFile(this.baseDir, ".storage-check-", ".tmp");

      try {
        Files.writeString(probe, "storage-check");
        Files.readString(probe);
      } finally {
        Files.deleteIfExists(probe);
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          EnrollmentMessages.ENROLLMENT_STORAGE_UNAVAILABLE + this.baseDir, e);
    }
  }

  @Override
  public StoredFile store(UUID applicationId, MultipartFile file) {
    final var stored = EnrollmentFilePolicy.prepare(applicationId, file);
    final var safeFileName = stored.safeFileName();

    Path appDir = baseDir.resolve(applicationId.toString()).normalize();

    if (!appDir.startsWith(baseDir)) {
      throw new InvalidFileException(EnrollmentMessages.FILE_PATH_TRAVERSAL);
    }

    try {
      Files.createDirectories(appDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Path targetFile = appDir.resolve(safeFileName).normalize();

    if (!targetFile.startsWith(appDir)) {
      throw new InvalidFileException(EnrollmentMessages.FILE_PATH_TRAVERSAL);
    }

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      deletePhysicalFile(applicationId + "/" + safeFileName);

      throw new UncheckedIOException(e);
    }

    return stored;
  }

  public Resource loadAsResource(String storagePath) {
    Path filePath = resolveAndValidate(storagePath);

    if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
      throw new AttachmentNotFoundException(EnrollmentMessages.FILE_NOT_FOUND);
    }

    try {
      Resource resource = new UrlResource(filePath.toUri());

      if (resource.exists() && resource.isReadable()) {
        return resource;
      } else {
        throw new AttachmentNotFoundException(EnrollmentMessages.FILE_UNREADABLE);
      }
    } catch (MalformedURLException e) {
      throw new AttachmentNotFoundException(EnrollmentMessages.FILE_PATH_INVALID);
    }
  }

  public InputStream loadAsInputStream(String storagePath) {
    Path filePath = resolveAndValidate(storagePath);

    if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
      throw new AttachmentNotFoundException(EnrollmentMessages.FILE_NOT_FOUND);
    }

    try {
      return Files.newInputStream(filePath);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public boolean deletePhysicalFile(String storagePath) {
    if (storagePath == null || storagePath.isBlank()) {
      return false;
    }

    try {
      Path filePath = resolveAndValidate(storagePath);
      boolean deleted = Files.deleteIfExists(filePath);

      try {
        Files.deleteIfExists(filePath.getParent());
      } catch (Exception ignored) {
      }

      return deleted;
    } catch (Exception e) {
      log.debug("[EnrollmentStorage] Deferred file cleanup failed, path: {}", storagePath);

      return false;
    }
  }

  private Path resolveAndValidate(String storagePath) {
    if (storagePath == null || storagePath.isBlank()) {
      throw new InvalidFileException(EnrollmentMessages.FILE_PATH_REQUIRED);
    }

    Path resolved = baseDir.resolve(storagePath).normalize();

    if (!resolved.startsWith(baseDir)) {
      throw new InvalidFileException(EnrollmentMessages.FILE_OUTSIDE_STORAGE);
    }

    return resolved;
  }
}
