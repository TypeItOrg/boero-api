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
import lombok.Setter;

@Entity
@Table(
    name = "institutions",
    uniqueConstraints = @UniqueConstraint(name = "institutions_slug_unique", columnNames = "slug"))
@Getter
@Setter
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
}
