package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createUser;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class UserRepositoryTest {

  @Autowired private EntityManager entityManager;
  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("Should find user by document number within institution")
  void shouldFindUserByDocumentNumberWithinInstitution() {
    Institution institution = createInstitution(entityManager, "boero");
    User user = createUser(entityManager, institution, "12345678");
    entityManager.flush();

    assertThat(
            userRepository.findByPersonDocumentNumberAndInstitution_Id(
                "12345678", institution.getId()))
        .contains(user);
  }

  @Test
  @DisplayName("Should not find user from another institution")
  void shouldNotFindUserFromAnotherInstitution() {
    Institution boero = createInstitution(entityManager, "boero");
    Institution other = createInstitution(entityManager, "other-school");
    createUser(entityManager, boero, "12345678");
    entityManager.flush();

    assertThat(userRepository.findByPersonDocumentNumberAndInstitution_Id("12345678", other.getId()))
        .isEmpty();
  }

  @Test
  @DisplayName("Should check user existence by document number within institution")
  void shouldCheckUserExistenceByDocumentNumberWithinInstitution() {
    Institution institution = createInstitution(entityManager, "boero");
    createUser(entityManager, institution, "12345678");
    entityManager.flush();

    assertThat(
            userRepository.existsByPersonDocumentNumberAndInstitution_Id(
                "12345678", institution.getId()))
        .isTrue();
    assertThat(
            userRepository.existsByPersonDocumentNumberAndInstitution_Id(
                "87654321", institution.getId()))
        .isFalse();
  }
}
