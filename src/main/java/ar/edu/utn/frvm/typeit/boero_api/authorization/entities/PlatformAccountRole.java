package ar.edu.utn.frvm.typeit.boero_api.authorization.entities;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "platform_account_roles",
    uniqueConstraints =
        @UniqueConstraint(
            name = "platform_account_roles_unique",
            columnNames = {"platform_account_id", "role_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PlatformAccountRole extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "platform_account_role_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "platform_account_id", nullable = false)
  private PlatformAccount platformAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;
}
