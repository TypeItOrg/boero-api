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
    name = "addresses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "addresses_institution_id_id_unique",
            columnNames = {"institution_id", "address_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Address extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "address_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  @Column(nullable = false)
  private String street;

  @Column(length = 50)
  private String number;

  @Column(length = 50)
  private String floor;

  @Column(length = 50)
  private String apartment;

  private String neighborhood;

  @Column(name = "additional_info")
  private String additionalInfo;
}
