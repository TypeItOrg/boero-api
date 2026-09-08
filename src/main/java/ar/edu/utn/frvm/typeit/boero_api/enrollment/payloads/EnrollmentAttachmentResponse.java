package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(requiredProperties = {"id", "attachmentType", "originalFileName", "size", "createdAt"})
public record EnrollmentAttachmentResponse(
    UUID id,
    EnrollmentAttachmentType attachmentType,
    String originalFileName,
    Long size,
    LocalDateTime createdAt) {

  public static EnrollmentAttachmentResponse from(final EnrollmentAttachment attachment) {
    return new EnrollmentAttachmentResponse(
        attachment.getId(),
        attachment.getAttachmentType(),
        attachment.getOriginalFileName(),
        attachment.getFileSize(),
        attachment.getCreatedAt());
  }
}
