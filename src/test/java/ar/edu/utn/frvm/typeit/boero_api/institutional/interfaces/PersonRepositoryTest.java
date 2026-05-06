package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.persist;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.person;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class PersonRepositoryTest {

  @Autowired private EntityManager entityManager;
  @Autowired private PersonRepository personRepository;

  @Test
  @DisplayName("Should find person by document number within institution")
  void shouldFindPersonByDocumentNumberWithinInstitution() {
    Institution institution = createInstitution(entityManager, "boero");
    Person person = persist(entityManager, person(institution, "12345678"));
    entityManager.flush();

    assertThat(personRepository.findByDocumentNumberAndInstitution_Id("12345678", institution.getId()))
        .contains(person);
  }

  @Test
  @DisplayName("Should isolate document lookup by institution")
  void shouldIsolateDocumentLookupByInstitution() {
    Institution boero = createInstitution(entityManager, "boero");
    Institution other = createInstitution(entityManager, "other-school");
    persist(entityManager, person(boero, "12345678"));
    entityManager.flush();

    assertThat(personRepository.findByDocumentNumberAndInstitution_Id("12345678", other.getId()))
        .isEmpty();
    assertThat(personRepository.existsByDocumentNumberAndInstitution_Id("12345678", boero.getId()))
        .isTrue();
    assertThat(personRepository.existsByDocumentNumberAndInstitution_Id("12345678", other.getId()))
        .isFalse();
  }
}
