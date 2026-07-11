package ar.edu.utn.frvm.typeit.boero_api.auth.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "user_sessions",
    indexes = @Index(name = "user_sessions_user_active_idx", columnList = "user_id, active"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserSession {

  @Id
  @GeneratedUUIDv7
  @Column(name = "user_session_id")
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", insertable = false, updatable = false, nullable = false)
  private User user;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  @CreatedDate
  @Column(name = "started_at", nullable = false, updatable = false)
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "remember_me", nullable = false)
  @Builder.Default
  private boolean rememberMe = false;
}
