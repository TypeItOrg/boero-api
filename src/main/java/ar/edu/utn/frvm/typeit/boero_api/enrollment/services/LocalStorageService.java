package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class LocalStorageService {

  public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L; // 10MB

  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/png", "image/jpg");

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png");

  private final Path baseDir;

  public LocalStorageService(
      @Value("${app.storage.enrollment.base-dir:storage/enrollments}") String baseDirStr) {
    this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.baseDir);
    } catch (IOException e) {
      log.error("Could not initialize enrollment storage directory at {}", this.baseDir, e);
    }
  }

  public record StoredFile(
      String safeFileName, String storagePath, String contentType, long size) {}

  public StoredFile store(UUID applicationId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidFileException("El archivo no puede estar vacío.");
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new InvalidFileException("El tamaño del archivo no puede superar los 10MB.");
    }

    String rawContentType = file.getContentType();
    if (rawContentType == null || rawContentType.isBlank()) {
      throw new InvalidFileException("Tipo de archivo no especificado.");
    }

    String contentType = rawContentType.toLowerCase(Locale.ROOT).trim();
    if (!ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new InvalidFileException(
          "Tipo de archivo no permitido. Solo se permiten formatos PDF, JPG y PNG.");
    }

    String extension = resolveExtension(file.getOriginalFilename(), contentType);
    String safeFileName = UUID.randomUUID() + extension;

    Path appDir = baseDir.resolve(applicationId.toString()).normalize();
    if (!appDir.startsWith(baseDir)) {
      throw new InvalidFileException("Intento de path traversal detectado.");
    }

    try {
      Files.createDirectories(appDir);
    } catch (IOException e) {
      log.error("Error al crear directorio para solicitud {}", applicationId, e);
      throw new InvalidFileException("Error de almacenamiento en el servidor.");
    }

    Path targetFile = appDir.resolve(safeFileName).normalize();
    if (!targetFile.startsWith(appDir)) {
      throw new InvalidFileException("Intento de path traversal detectado.");
    }

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("Error al guardar archivo en {}", targetFile, e);
      throw new InvalidFileException("Error al guardar archivo en almacenamiento.");
    }

    String relativeStoragePath = applicationId + "/" + safeFileName;
    return new StoredFile(safeFileName, relativeStoragePath, contentType, file.getSize());
  }

  public Resource loadAsResource(String storagePath) {
    Path filePath = resolveAndValidate(storagePath);
    if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
      throw new AttachmentNotFoundException("No se encontró el archivo físico en almacenamiento.");
    }

    try {
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      } else {
        throw new AttachmentNotFoundException("No se pudo leer el archivo físico.");
      }
    } catch (MalformedURLException e) {
      throw new AttachmentNotFoundException("Ruta de archivo inválida.");
    }
  }

  public InputStream loadAsInputStream(String storagePath) {
    Path filePath = resolveAndValidate(storagePath);
    if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
      throw new AttachmentNotFoundException("No se encontró el archivo físico en almacenamiento.");
    }

    try {
      return Files.newInputStream(filePath);
    } catch (IOException e) {
      log.error("Error al abrir stream para {}", filePath, e);
      throw new AttachmentNotFoundException("Error al leer el stream del archivo.");
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
      log.warn("No se pudo eliminar el archivo físico en {}: {}", storagePath, e.getMessage());
      return false;
    }
  }

  private Path resolveAndValidate(String storagePath) {
    if (storagePath == null || storagePath.isBlank()) {
      throw new InvalidFileException("Ruta de archivo no especificada.");
    }
    Path resolved = baseDir.resolve(storagePath).normalize();
    if (!resolved.startsWith(baseDir)) {
      throw new InvalidFileException("Acceso no permitido fuera del directorio de almacenamiento.");
    }
    return resolved;
  }

  private String resolveExtension(String originalFilename, String contentType) {
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension =
          originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    if (ALLOWED_EXTENSIONS.contains(extension)) {
      return extension;
    }

    return switch (contentType) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      case "image/jpeg", "image/jpg" -> ".jpg";
      default -> throw new InvalidFileException("Extensión de archivo no permitida.");
    };
  }
}
