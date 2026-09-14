package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(
    requiredProperties = {
      "id",
      "attachmentType",
      "originalFileName",
      "storagePath",
      "contentType",
      "fileSize",
      "createdAt"
    })
public record AttachmentDto(
    @Schema(nullable = true) UUID id,
    @Schema(nullable = true) String attachmentType,
    @Schema(nullable = true) String originalFileName,
    @Schema(nullable = true) String storagePath,
    @Schema(nullable = true) String contentType,
    @Schema(nullable = true) Long fileSize,
    @Schema(nullable = true) Instant createdAt) {
  public AttachmentDto() {
    this(null, null, null, null, null, null, null);
  }

  public UUID getId() {
    return id;
  }

  public String getAttachmentType() {
    return attachmentType;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
