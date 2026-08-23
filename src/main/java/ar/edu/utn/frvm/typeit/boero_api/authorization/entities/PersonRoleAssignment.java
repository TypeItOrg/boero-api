package ar.edu.utn.frvm.typeit.boero_api.authorization.entities;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.PERSON_ROLE_INSTITUTION_MISMATCH;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    name = "person_role_assignments",
    uniqueConstraints =
        @UniqueConstraint(
            name = "person_role_assignments_unique",
            columnNames = {"person_id", "role_id", "institution_id"}),
    indexes = {
      @Index(
          name = "person_role_assignments_person_institution_idx",
          columnList = "person_id, institution_id"),
      @Index(
          name = "person_role_assignments_institution_role_idx",
          columnList = "institution_id, role_id")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PersonRoleAssignment extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "person_role_assignment_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  public static PersonRoleAssignment assign(
      final Person person, final Role role, final Institution institution) {
    final PersonRoleAssignment assignment =
        PersonRoleAssignment.builder().person(person).role(role).institution(institution).build();
    assignment.validateInstitutionConsistency();
    return assignment;
  }

  public void replaceRole(final Role role) {
    this.role = role;
    validateInstitutionConsistency();
  }

  @PrePersist
  @PreUpdate
  private void validateInstitutionConsistency() {
    final UUID institutionId = institution.getId();
    final UUID roleInstitutionId =
        role.getInstitution() == null ? null : role.getInstitution().getId();
    final boolean personMatches = institutionId.equals(person.getInstitution().getId());
    final boolean roleMatches =
        roleInstitutionId == null || institutionId.equals(roleInstitutionId);

    if (!personMatches || !roleMatches) {
      throw new IllegalStateException(PERSON_ROLE_INSTITUTION_MISMATCH);
    }
  }
}
