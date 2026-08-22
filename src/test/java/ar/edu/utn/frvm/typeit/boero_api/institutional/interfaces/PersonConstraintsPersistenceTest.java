package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class PersonConstraintsPersistenceTest {

  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("Should reject persist when first name is shorter than minimum (Bean Validation)")
  void shouldRejectPersistWhenFirstNameTooShort() {
    Institution institution = createInstitution(entityManager, "person-bv-short-name");
    Person person =
        Person.builder()
            .institution(institution)
            .firstName("AB")
            .lastName("Garcia")
            .documentNumber("12345678")
            .email("ana@example.com")
            .build();
    assertPersistFlushFailsBeanValidation(person);
  }

  @Test
  @DisplayName("Should reject persist when document format is invalid (Bean Validation)")
  void shouldRejectPersistWhenDocumentFormatInvalid() {
    Institution institution = createInstitution(entityManager, "person-bv-doc");
    Person person =
        Person.builder()
            .institution(institution)
            .firstName("Ana")
            .lastName("Garcia")
            .documentNumber("abcdefgh")
            .email("ana@example.com")
            .build();
    assertPersistFlushFailsBeanValidation(person);
  }

  @Test
  @DisplayName("Should reject native insert when document violates CHECK constraint")
  void shouldRejectNativeInsertWhenDocumentViolatesCheckConstraint() {
    Institution institution = createInstitution(entityManager, "person-check-doc");
    entityManager.flush();

    UUID personId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                entityManager
                    .createNativeQuery(
                        """
                        INSERT INTO people (
                          person_id, institution_id, document_number, first_name, last_name,
                          created_at, updated_at)
                        VALUES (
                          ?1, ?2, '1234567', 'Ana', 'Garcia',
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                    .setParameter(1, personId)
                    .setParameter(2, institution.getId())
                    .executeUpdate())
        .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
  }

  private void assertPersistFlushFailsBeanValidation(Person person) {
    assertThatThrownBy(
            () -> {
              entityManager.persist(person);
              entityManager.flush();
            })
        .isInstanceOf(ConstraintViolationException.class);
  }
}
