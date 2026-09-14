package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentStorage.StoredFile;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public final class EnrollmentFilePolicy {

  public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;
  private static final int MAX_FILE_NAME_LENGTH = 255;

  private EnrollmentFilePolicy() {}

  public static StoredFile prepare(final UUID applicationId, final MultipartFile file) {
    validateFile(file);

    final String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
    final String fileName = UUID.randomUUID() + extensionFor(contentType);
    final String storagePath = applicationId + "/" + fileName;

    return new StoredFile(fileName, storagePath, contentType, file.getSize());
  }

  private static void validateFile(final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidFileException(EnrollmentMessages.FILE_EMPTY);
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new InvalidFileException(EnrollmentMessages.FILE_TOO_LARGE);
    }

    final String originalFileName = file.getOriginalFilename();
    if (originalFileName != null && originalFileName.length() > MAX_FILE_NAME_LENGTH) {
      throw new InvalidFileException(EnrollmentMessages.ATTACHMENT_NAME_TOO_LONG);
    }

    final String contentType = file.getContentType();
    if (contentType == null || contentType.isBlank()) {
      throw new InvalidFileException(EnrollmentMessages.FILE_TYPE_REQUIRED);
    }
  }

  private static String extensionFor(final String contentType) {
    return switch (contentType) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      case "image/jpeg", "image/jpg" -> ".jpg";
      default -> throw new InvalidFileException(EnrollmentMessages.FILE_TYPE_INVALID);
    };
  }
}
