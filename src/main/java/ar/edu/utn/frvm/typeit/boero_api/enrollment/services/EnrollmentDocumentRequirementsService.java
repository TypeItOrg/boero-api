package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.TrainingPathDocumentRequirementRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentDocumentRequirement;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentReviewStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentValidationException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.interfaces.EnrollmentAttachmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAttachmentResponse;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentDocumentRequirementResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentDocumentRequirementsService {
  private final TrainingPathDocumentRequirementRepository definitions;
  private final EnrollmentAttachmentRepository attachments;
  private final EnrollmentDocumentAuthorization authorization;
  private final EnrollmentApplicationPeriodService periods;

  @Transactional(propagation = Propagation.MANDATORY)
  public void snapshot(EnrollmentApplication application) {
    definitions
        .findByTrainingPathIdOrderByDisplayOrderAscIdAsc(application.getTrainingPathId())
        .stream()
        .filter(value -> value.isActive())
        .forEach(
            value ->
                application.addDocumentRequirement(
                    EnrollmentDocumentRequirement.snapshot(application, value)));
  }

  public EnrollmentDocumentRequirement requirement(EnrollmentApplication application, UUID id) {
    return application.getDocumentRequirements().stream()
        .filter(value -> value.getId().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new EnrollmentValidationException(
                    EnrollmentMessages.DOCUMENT_REQUIREMENT_NOT_FOUND));
  }

  public void requireSubmission(EnrollmentApplication application) {
    var current = current(application);
    if (application.getDocumentRequirements().stream()
        .anyMatch(
            value ->
                value.getLevel() == DocumentRequirementLevel.AT_SUBMISSION
                    && !current.containsKey(value.getId()))) {
      throw new EnrollmentValidationException(EnrollmentMessages.DOCUMENT_SUBMISSION_REQUIRED);
    }
  }

  public boolean initialAccepted(EnrollmentApplication application) {
    return accepted(application, true);
  }

  public boolean allAccepted(EnrollmentApplication application) {
    return accepted(application, false);
  }

  private boolean accepted(EnrollmentApplication application, boolean initialOnly) {
    var current = current(application);
    return application.getDocumentRequirements().stream()
        .filter(
            value ->
                initialOnly
                    ? value.getLevel() == DocumentRequirementLevel.AT_SUBMISSION
                    : value.getLevel() != DocumentRequirementLevel.OPTIONAL)
        .allMatch(
            value ->
                current.containsKey(value.getId())
                    && current.get(value.getId()).getReviewStatus()
                        == DocumentReviewStatus.ACCEPTED);
  }

  public void requireApproval(EnrollmentApplication application, boolean provisional) {
    if (provisional
        ? !initialAccepted(application) || allAccepted(application)
        : !allAccepted(application)) {
      throw new EnrollmentValidationException(
          provisional
              ? EnrollmentMessages.DOCUMENT_PROVISIONAL_REQUIRED
              : EnrollmentMessages.DOCUMENT_CONFIRMATION_REQUIRED);
    }
  }

  public boolean editable(EnrollmentApplication application) {
    return application.getDeletedAt() == null
        && (application.getStatus() == EnrollmentApplicationStatus.SUBMITTED
            || application.getStatus() == EnrollmentApplicationStatus.PROVISIONALLY_APPROVED
            || application.isEditable() && periods.isOpen(application));
  }

  public List<EnrollmentDocumentRequirementResponse> responses(
      EnrollmentApplication application, @Nullable Authentication authentication) {
    var current = current(application);
    boolean editable = editable(application);
    boolean upload =
        editable
            && authorization.canAccess(
                application, authentication, PermissionCode.ENROLLMENT_ATTACHMENT_UPLOAD);
    boolean delete =
        editable
            && authorization.canAccess(
                application, authentication, PermissionCode.ENROLLMENT_ATTACHMENT_DELETE);
    boolean review =
        application.getStatus() != EnrollmentApplicationStatus.DRAFT
            && editable
            && authorization.canAccess(
                application, authentication, PermissionCode.ENROLLMENT_ATTACHMENT_REVIEW);
    return application.getDocumentRequirements().stream()
        .sorted(
            Comparator.comparingInt(
                    (EnrollmentDocumentRequirement mappedEnrollmentDocumentRequirement) ->
                        mappedEnrollmentDocumentRequirement.getDisplayOrder())
                .thenComparing(
                    (EnrollmentDocumentRequirement mappedEnrollmentDocumentRequirement) ->
                        mappedEnrollmentDocumentRequirement.getId()))
        .map(
            value -> {
              var file = current.get(value.getId());
              boolean mutable =
                  file == null || file.getReviewStatus() != DocumentReviewStatus.ACCEPTED;
              return new EnrollmentDocumentRequirementResponse(
                  value.getId(),
                  value.getName(),
                  value.getInstructions(),
                  value.getLevel(),
                  value.getAllowedFormats(),
                  value.getDisplayOrder(),
                  file == null ? "MISSING" : file.getReviewStatus().name(),
                  file == null ? null : EnrollmentAttachmentResponse.from(file),
                  upload && file == null,
                  upload && delete && file != null && mutable,
                  delete
                      && file != null
                      && mutable
                      && (application.isEditable()
                          || value.getLevel() != DocumentRequirementLevel.AT_SUBMISSION),
                  review
                      && file != null
                      && file.getReviewStatus() == DocumentReviewStatus.PENDING_REVIEW);
            })
        .toList();
  }

  private Map<UUID, EnrollmentAttachment> current(EnrollmentApplication application) {
    Map<UUID, EnrollmentAttachment> result = new HashMap<>();
    for (var file :
        attachments.findByEnrollmentApplicationIdAndDeletedAtIsNull(application.getId())) {
      if (file.isCurrent()) {
        result.put(file.getRequirement().getId(), file);
      }
    }
    return result;
  }
}
