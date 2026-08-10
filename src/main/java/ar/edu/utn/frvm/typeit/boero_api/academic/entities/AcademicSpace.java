package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "academic_spaces",
    uniqueConstraints =
        @UniqueConstraint(
            name = "academic_spaces_institution_id_id_unique",
            columnNames = {"institution_id", "academic_space_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AcademicSpace extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "academic_space_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AcademicSpaceType type;

  @Column(nullable = false)
  private boolean active;

  public static AcademicSpace create(
      final Institution institution,
      final String name,
      final String description,
      final AcademicSpaceType type) {
    return AcademicSpace.builder()
        .institution(institution)
        .name(AcademicNameNormalizer.display(name))
        .description(description)
        .type(type)
        .active(true)
        .build();
  }

  public void update(final String name, final String description, final AcademicSpaceType type) {
    this.name = AcademicNameNormalizer.display(name);
    this.description = description;
    this.type = type;
  }

  public void updateStatus(final boolean active) {
    this.active = active;
  }
}
