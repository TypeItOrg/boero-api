package ar.edu.utn.frvm.typeit.boero_api.institutional.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
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
import lombok.Setter;

@Entity
@Table(
    name = "guardian_profiles",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "guardian_profiles_institution_id_id_unique",
          columnNames = {"institution_id", "guardian_profile_id"}),
      @UniqueConstraint(
          name = "guardian_profiles_person_id_unique",
          columnNames = {"institution_id", "person_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GuardianProfile extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "guardian_profile_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @Column(length = 100)
  private String occupation;

  @Enumerated(EnumType.STRING)
  @Column(name = "education_level", length = 40)
  private EducationLevel educationLevel;
}
