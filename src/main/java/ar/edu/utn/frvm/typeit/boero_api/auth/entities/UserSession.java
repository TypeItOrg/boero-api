package ar.edu.utn.frvm.typeit.boero_api.auth.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_session_id")
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

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

  @PrePersist
  protected void onCreate() {
    startedAt = LocalDateTime.now();
  }
}
