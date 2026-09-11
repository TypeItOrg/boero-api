package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.ApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.InvalidEnrollmentApplicationStateException;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.MissingRejectionReasonException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EnrollmentApplicationTest {

  private static final LocalDateTime RESOLVED_AT = LocalDateTime.of(2026, 9, 6, 10, 0);

  @Test
  void submit_transitionsDraftToSubmitted() {
    final var application = draft();

    application.submit();

    assertThat(application.getStatus()).isEqualTo(EnrollmentApplicationStatus.SUBMITTED);
  }

  @Test
  void submit_rejectsAlreadySubmittedApplication() {
    final var application = submitted();

    assertThatThrownBy(application::submit)
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void cancel_transitionsDraftToCancelled() {
    final var application = draft();

    application.cancel();

    assertThat(application.getStatus()).isEqualTo(EnrollmentApplicationStatus.CANCELLED);
  }

  @Test
  void cancel_rejectsNonDraftApplication() {
    final var application = submitted();

    assertThatThrownBy(application::cancel).isInstanceOf(ApplicationNotEditableException.class);
  }

  @Test
  void approve_transitionsSubmittedToApproved() {
    final var application = submitted();

    application.approve(RESOLVED_AT);

    assertThat(application.isApproved()).isTrue();
    assertThat(application.getStatus()).isEqualTo(EnrollmentApplicationStatus.APPROVED);
    assertThat(application.getResolvedAt()).isEqualTo(RESOLVED_AT);
  }

  @Test
  void approve_rejectsDraftApplication() {
    final var application = draft();

    assertThatThrownBy(() -> application.approve(RESOLVED_AT))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void approve_rejectsAlreadyResolvedApplication() {
    final var application = submitted();
    application.approve(RESOLVED_AT);

    assertThatThrownBy(() -> application.approve(RESOLVED_AT.plusHours(1)))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void reject_transitionsSubmittedToRejectedStoringReason() {
    final var application = submitted();

    application.reject("Documentación incompleta", RESOLVED_AT);

    assertThat(application.getStatus()).isEqualTo(EnrollmentApplicationStatus.REJECTED);
    assertThat(application.getRejectionReason()).isEqualTo("Documentación incompleta");
    assertThat(application.getResolvedAt()).isEqualTo(RESOLVED_AT);
  }

  @Test
  void reject_requiresNonBlankReason() {
    final var application = submitted();

    assertThatThrownBy(() -> application.reject("   ", RESOLVED_AT))
        .isInstanceOf(MissingRejectionReasonException.class);
    assertThatThrownBy(() -> application.reject(null, RESOLVED_AT))
        .isInstanceOf(MissingRejectionReasonException.class);
    assertThat(application.getStatus()).isEqualTo(EnrollmentApplicationStatus.SUBMITTED);
  }

  @Test
  void reject_rejectsAlreadyResolvedApplication() {
    final var application = submitted();
    application.reject("Documentación incompleta", RESOLVED_AT);

    assertThatThrownBy(() -> application.reject("Otro motivo", RESOLVED_AT.plusHours(1)))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void updateEducationBackground_createsBackgroundOnFirstUpdate() {
    final var application = draft();

    application.updateEducationBackground("Escuela N° 1");

    assertThat(application.getEducationBackground()).isNotNull();
    assertThat(application.getEducationBackground().getSecondarySchool()).isEqualTo("Escuela N° 1");
  }

  @Test
  void updateEducationBackground_rejectsNonEditableApplication() {
    final var application = submitted();

    assertThatThrownBy(() -> application.updateEducationBackground("Escuela N° 1"))
        .isInstanceOf(InvalidEnrollmentApplicationStateException.class);
  }

  @Test
  void resolvedApplicationIsNotEditable() {
    final var application = submitted();
    application.approve(RESOLVED_AT);

    assertThat(application.isEditable()).isFalse();
    assertThat(application.isPendingEvaluation()).isFalse();
    assertThat(application.isResolved()).isTrue();
  }

  private static EnrollmentApplication draft() {
    return EnrollmentApplication.create(null, null, null, null, null);
  }

  private static EnrollmentApplication submitted() {
    final var application = draft();
    application.submit();
    return application;
  }
}
