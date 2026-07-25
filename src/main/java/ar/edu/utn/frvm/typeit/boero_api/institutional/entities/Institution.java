package ar.edu.utn.frvm.typeit.boero_api.institutional.entities;

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

@Entity
@Table(
    name = "institutions",
    uniqueConstraints = @UniqueConstraint(name = "institutions_slug_unique", columnNames = "slug"))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Institution extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "institution_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 100)
  private String slug;

  private String street;

  @Column(length = 50)
  private String number;

  private String neighborhood;

  @Column(name = "additional_info")
  private String additionalInfo;

  @Column(name = "phone_number", length = 30)
  private String phoneNumber;

  @Column(length = 150)
  private String email;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  public void rename(final String name) {
    this.name = name;
  }

  public void changeSlug(final String slug) {
    this.slug = slug;
  }

  public void updateLocation(
      final City city,
      final String street,
      final String number,
      final String neighborhood,
      final String additionalInfo) {
    this.city = city;
    this.street = street;
    this.number = number;
    this.neighborhood = neighborhood;
    this.additionalInfo = additionalInfo;
  }

  public void updateContact(final String phoneNumber, final String email) {
    this.phoneNumber = phoneNumber;
    this.email = email;
  }

  public boolean updateStatus(final boolean active) {
    if (this.active == active) {
      return false;
    }

    this.active = active;
    return true;
  }
}
