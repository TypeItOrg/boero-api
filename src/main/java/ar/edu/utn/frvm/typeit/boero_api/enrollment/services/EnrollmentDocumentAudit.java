package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentDocumentAction;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentDocumentAudit {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public record Actor(UUID id, String accountType, String requestId) {
    public static Actor from(final Authentication authentication) {
      final String requestId = MDC.get("requestId");
      if (authentication != null
          && authentication.getPrincipal() instanceof JwtAuthenticatedUser user) {
        return new Actor(user.personId(), "INSTITUTION", requestId);
      }
      if (authentication != null
          && authentication.getPrincipal() instanceof JwtAuthenticatedPlatformAccount account) {
        return new Actor(account.platformAccountId(), "PLATFORM", requestId);
      }
      return new Actor(null, "ANONYMOUS", requestId);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      final Actor actor,
      final UUID institutionId,
      final UUID applicationId,
      final UUID attachmentId,
      final EnrollmentDocumentAction action,
      final String result) {
    jdbc.update(
        """
        INSERT INTO enrollment_document_audit
          (id, occurred_at, actor_id, account_type, institution_id, application_id, attachment_id, action, result, request_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        Timestamp.from(clock.instant()),
        actor.id(),
        actor.accountType(),
        institutionId,
        applicationId,
        attachmentId,
        action.name(),
        result,
        actor.requestId());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordIndependent(
      final Authentication authentication,
      final UUID institutionId,
      final UUID applicationId,
      final UUID attachmentId,
      final EnrollmentDocumentAction action,
      final String result) {
    record(Actor.from(authentication), institutionId, applicationId, attachmentId, action, result);
  }
}
