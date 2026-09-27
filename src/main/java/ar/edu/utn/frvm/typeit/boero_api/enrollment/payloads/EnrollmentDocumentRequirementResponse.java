package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(
    requiredProperties = {
      "id",
      "name",
      "instructions",
      "level",
      "allowedFormats",
      "displayOrder",
      "status",
      "currentAttachment",
      "canUpload",
      "canReplace",
      "canWithdraw",
      "canReview"
    })
public record EnrollmentDocumentRequirementResponse(
    UUID id,
    String name,
    String instructions,
    DocumentRequirementLevel level,
    List<String> allowedFormats,
    int displayOrder,
    String status,
    @Schema(nullable = true) @Nullable EnrollmentAttachmentResponse currentAttachment,
    boolean canUpload,
    boolean canReplace,
    boolean canWithdraw,
    boolean canReview) {}
