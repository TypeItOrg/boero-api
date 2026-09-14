package ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces;

import java.io.InputStream;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface EnrollmentStorage {
  record StoredFile(String safeFileName, String storagePath, String contentType, long size) {}

  StoredFile store(UUID applicationId, MultipartFile file);

  Resource loadAsResource(String storagePath);

  InputStream loadAsInputStream(String storagePath);

  boolean deletePhysicalFile(String storagePath);
}
