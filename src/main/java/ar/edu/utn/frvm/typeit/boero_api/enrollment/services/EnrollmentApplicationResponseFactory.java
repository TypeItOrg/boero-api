package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentDocumentAction;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentApplicationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentApplicationResponseFactory {
  private final EnrollmentApplicationPeriodService periods;
  private final EnrollmentDocumentAuthorization authorization;
  private final EnrollmentDocumentAudit audit;
  private final EnrollmentDocumentRequirementsService documents;
  private final EnrollmentAdmissionHistory history;

  public EnrollmentApplicationResponse from(final EnrollmentApplication application) {
    return from(application, true);
  }

  public EnrollmentApplicationResponse from(
      final EnrollmentApplication application, final boolean includeCourses) {
    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    final boolean canRead =
        authorization.canAccess(
            application, authentication, PermissionCode.ENROLLMENT_ATTACHMENT_READ);
    final boolean canApprove =
        authorization.canAccess(
            application, authentication, PermissionCode.ENROLLMENT_APPLICATION_APPROVE);
    if (canRead && !application.getDocumentRequirements().isEmpty()) {
      audit.recordIndependent(
          authentication,
          application.getInstitution().getId(),
          application.getId(),
          null,
          EnrollmentDocumentAction.METADATA_READ,
          "ALLOWED");
    }

    final var response =
        EnrollmentApplicationResponse.from(application, includeCourses, canRead).toBuilder()
            .admissionHistory(history.list(application.getId()))
            .periodOpen(periods.isOpen(application))
            .documents(canRead ? documents.responses(application, authentication) : List.of());
    final boolean canConfirm =
        canRead
            && canApprove
            && (application.isPendingEvaluation()
                || application.getStatus() == EnrollmentApplicationStatus.PROVISIONALLY_APPROVED)
            && documents.allAccepted(application);

    return response
        .canApproveProvisionally(
            canRead
                && canApprove
                && application.isPendingEvaluation()
                && documents.initialAccepted(application)
                && !canConfirm)
        .canConfirm(canConfirm)
        .build();
  }
}
