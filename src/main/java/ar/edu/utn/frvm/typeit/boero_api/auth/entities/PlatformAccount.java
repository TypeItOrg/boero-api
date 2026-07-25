package ar.edu.utn.frvm.typeit.boero_api.auth.entities;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(
    name = "platform_accounts",
    uniqueConstraints =
        @UniqueConstraint(name = "platform_accounts_email_unique", columnNames = "email"))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PlatformAccount extends Auditable implements UserDetails {

  @Id
  @GeneratedUUIDv7
  @Column(name = "platform_account_id")
  private UUID id;

  @Column(nullable = false, length = 150)
  private String email;

  @NotBlank(message = "El nombre es requerido.")
  @Size.List({
    @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres."),
    @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
  })
  @Pattern(regexp = NAME_PATTERN, message = "El nombre solo puede contener letras y espacios.")
  @Column(name = "first_name", nullable = false, length = NAME_MAX)
  private String name;

  @NotBlank(message = "El apellido es requerido.")
  @Size.List({
    @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres."),
    @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
  })
  @Pattern(regexp = NAME_PATTERN, message = "El apellido solo puede contener letras y espacios.")
  @Column(name = "last_name", nullable = false, length = NAME_MAX)
  private String lastName;

  @Column(nullable = false, length = 100)
  private String password;

  @Column(nullable = false)
  @Builder.Default
  private boolean enabled = true;

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  public void updateProfile(final String name, final String lastName, final String email) {
    this.name = name;
    this.lastName = lastName;
    this.email = email;
  }

  public void changePassword(final String password) {
    this.password = password;
  }

  public boolean updateAccess(final boolean enabled) {
    if (this.enabled == enabled) {
      return false;
    }

    this.enabled = enabled;
    return true;
  }
}
