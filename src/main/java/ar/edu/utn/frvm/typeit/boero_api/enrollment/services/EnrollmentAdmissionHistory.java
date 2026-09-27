package ar.edu.utn.frvm.typeit.boero_api.enrollment.services;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads.EnrollmentAdmissionHistoryResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentAdmissionHistory {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(EnrollmentApplication application) {
    var actor =
        EnrollmentDocumentAudit.Actor.from(SecurityContextHolder.getContext().getAuthentication());
    jdbc.update(
        "INSERT INTO enrollment_admission_history(id,application_id,status,occurred_at,actor_id,account_type) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        application.getId(),
        application.getStatus().name(),
        Timestamp.from(clock.instant()),
        actor.id(),
        actor.accountType());
  }

  @Transactional(readOnly = true)
  public List<EnrollmentAdmissionHistoryResponse> list(UUID applicationId) {
    return jdbc.query(
        "SELECT * FROM enrollment_admission_history WHERE application_id=? ORDER BY occurred_at,id",
        (row, index) ->
            new EnrollmentAdmissionHistoryResponse(
                row.getObject("id", UUID.class),
                row.getString("status"),
                row.getTimestamp("occurred_at").toInstant(),
                row.getObject("actor_id", UUID.class),
                row.getString("account_type")),
        applicationId);
  }
}
