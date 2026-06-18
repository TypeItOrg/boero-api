package ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.persist;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.person;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AssignPersonSystemRoleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.PermissionRoleSeed;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  JpaAuditingTestConfig.class,
  PermissionRoleSeed.class,
  AssignPersonSystemRoleUseCase.class,
  SessionRevocationService.class
})
class PersonRoleAssignmentRepositoryTest {

  @Autowired private EntityManager entityManager;
  @Autowired private PermissionRoleSeed permissionRoleSeed;
  @Autowired private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  private Institution institution;
  private Person authority;
  private Person teacher;

  @BeforeEach
  void setUp() {
    permissionRoleSeed.run(null);
    institution = createInstitution(entityManager, "boero");
    authority = persist(entityManager, person(institution, "11111111"));
    teacher = persist(entityManager, person(institution, "22222222"));
    assignPersonSystemRoleUseCase.execute(authority, SystemRoleCode.INSTITUTIONAL_AUTHORITY, false);
    assignPersonSystemRoleUseCase.execute(teacher, SystemRoleCode.TEACHER, false);
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("Should list person role assignments with role loaded")
  void findByPersonIdAndInstitutionId_loadsRole() {
    List<PersonRoleAssignment> assignments =
        personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(
            teacher.getId(), institution.getId());

    assertThat(assignments).hasSize(1);
    assertThat(assignments.getFirst().getRole().getCode()).isEqualTo(SystemRoleCode.TEACHER.name());
    assertThat(assignments.getFirst().getRole().getName()).isEqualTo("Docente");
  }

  @Test
  @DisplayName("Should count institutional authorities in institution")
  void countByInstitutionIdAndRoleCode_countsAuthorities() {
    assertThat(
            personRoleAssignmentRepository.countByInstitution_IdAndRole_Code(
                institution.getId(), SystemRoleCode.INSTITUTIONAL_AUTHORITY.name()))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Should list institutional system roles")
  void findInstitutionalSystemRoles_returnsCatalog() {
    List<Role> roles =
        roleRepository.findByScopeAndSystemTrueAndInstitutionIsNullOrderByNameAsc(
            RoleScope.INSTITUTION);

    assertThat(roles).hasSize(SystemRoleCode.values().length);
    assertThat(roles).extracting(Role::getCode).contains(SystemRoleCode.TEACHER.name());
  }

  @Test
  @DisplayName("Should count only active people with institutional authority role")
  void countActivePeopleByInstitutionIdAndRoleCode_countsOnlyNotDeletedPeople() {
    Person deletedAuthority = persist(entityManager, person(institution, "33333333"));
    deletedAuthority.setDeleted(true);
    entityManager.persist(deletedAuthority);
    Role authorityRole =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(
                RoleScope.INSTITUTION, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name())
            .orElseThrow();
    entityManager.persist(
        PersonRoleAssignment.builder()
            .person(deletedAuthority)
            .institution(institution)
            .role(authorityRole)
            .build());
    entityManager.flush();

    long count =
        personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institution.getId(), SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());

    assertThat(count).isEqualTo(1);
  }
}
