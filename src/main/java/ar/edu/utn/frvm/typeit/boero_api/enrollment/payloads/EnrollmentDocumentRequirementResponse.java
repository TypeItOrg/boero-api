package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(
    requiredProperties = {
      "sourceRequirementId",
      "appliedDefinitionRevision",
      "appliedAssignmentRevision",
      "origin",
      "requestId",
      "documentId",
      "active",
      "specificInstructions",
      "needsReplacement",
      "changes",
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
    boolean canReview,
    UUID documentId,
    boolean active,
    @Schema(nullable = true) @Nullable String specificInstructions,
    boolean needsReplacement,
    List<EnrollmentRequirementChangeResponse> changes,
    ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementOrigin origin,
    @Schema(nullable = true) @Nullable UUID requestId,
    @Schema(nullable = true) @Nullable UUID sourceRequirementId,
    @Schema(nullable = true) @Nullable Long appliedDefinitionRevision,
    @Schema(nullable = true) @Nullable Long appliedAssignmentRevision) {}
