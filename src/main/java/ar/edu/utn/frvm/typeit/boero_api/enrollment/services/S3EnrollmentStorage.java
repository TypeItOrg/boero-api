package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.AttachmentNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidFileException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.storage.enrollment.provider", havingValue = "s3")
public class S3EnrollmentStorage implements EnrollmentStorage {

  private static final Pattern STORAGE_PATH_PATTERN =
      Pattern.compile("[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.(pdf|jpg|jpeg|png)");

  private final S3Client client;
  private final String bucket;
  private final String prefix;

  public S3EnrollmentStorage(
      final S3Client client,
      @Value("${app.storage.enrollment.s3.bucket}") final String bucket,
      @Value("${app.storage.enrollment.s3.prefix:enrollments/}") final String prefix) {
    if (bucket.isBlank()) {
      throw new IllegalStateException(EnrollmentMessages.S3_BUCKET_REQUIRED);
    }

    this.client = client;
    this.bucket = bucket;
    this.prefix = prefix.isBlank() ? "" : prefix.replaceAll("/+$", "") + "/";

    verifyStorageAccess();
  }

  @Override
  public StoredFile store(final UUID applicationId, final MultipartFile file) {
    final var stored = EnrollmentFilePolicy.prepare(applicationId, file);

    try (final var input = file.getInputStream()) {
      client.putObject(
          request ->
              request
                  .bucket(bucket)
                  .key(resolveKey(stored.storagePath()))
                  .contentType(stored.contentType()),
          RequestBody.fromInputStream(input, stored.size()));

      return stored;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  public Resource loadAsResource(final String storagePath) {
    return new InputStreamResource(loadAsInputStream(storagePath));
  }

  @Override
  public InputStream loadAsInputStream(final String storagePath) {
    try {
      return client.getObject(request -> request.bucket(bucket).key(resolveKey(storagePath)));
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        throw new AttachmentNotFoundException(EnrollmentMessages.FILE_NOT_FOUND);
      }

      throw exception;
    }
  }

  @Override
  public boolean deletePhysicalFile(final String storagePath) {
    try {
      client.deleteObject(request -> request.bucket(bucket).key(resolveKey(storagePath)));

      return true;
    } catch (RuntimeException exception) {
      log.debug("[EnrollmentStorage] Deferred object cleanup failed, path: {}", storagePath);

      return false;
    }
  }

  private String resolveKey(final String storagePath) {
    if (storagePath == null || !STORAGE_PATH_PATTERN.matcher(storagePath).matches()) {
      throw new InvalidFileException(EnrollmentMessages.FILE_PATH_INVALID);
    }

    return prefix + storagePath;
  }

  private void verifyStorageAccess() {
    final String probeKey = prefix + ".storage-check-" + UUID.randomUUID();

    client.putObject(
        request -> request.bucket(bucket).key(probeKey), RequestBody.fromBytes(new byte[] {0}));

    try {
      client.getObjectAsBytes(request -> request.bucket(bucket).key(probeKey));
    } finally {
      client.deleteObject(request -> request.bucket(bucket).key(probeKey));
    }
  }
}
