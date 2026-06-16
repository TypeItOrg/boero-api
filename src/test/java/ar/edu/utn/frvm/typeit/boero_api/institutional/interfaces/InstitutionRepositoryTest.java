package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.city;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.country;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.createInstitution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.institution;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.persist;
import static ar.edu.utn.frvm.typeit.boero_api.support.InstitutionalTestData.province;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Country;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class InstitutionRepositoryTest {

  @Autowired private EntityManager entityManager;
  @Autowired private InstitutionRepository institutionRepository;

  @Test
  @DisplayName("Should list only active institutions with city and province loaded")
  void findByActiveTrue_returnsOnlyActiveInstitutionsWithLocation() {
    Institution active = createInstitution(entityManager, "boero-active");
    Institution inactive = createInstitution(entityManager, "boero-inactive");
    inactive.setActive(false);
    entityManager.merge(inactive);
    entityManager.flush();
    entityManager.clear();

    var page = institutionRepository.findByActiveTrue(PageRequest.of(0, 10));

    assertThat(page.getTotalElements()).isEqualTo(1);
    Institution found = page.getContent().getFirst();
    assertThat(found.getSlug()).isEqualTo("boero-active");
    assertThat(found.getCity().getName()).isEqualTo("Villa Maria");
    assertThat(found.getCity().getProvince().getName()).isEqualTo("Cordoba");
  }

  @Test
  @DisplayName("Should detect slug duplicates")
  void existsBySlug_detectsDuplicates() {
    createInstitution(entityManager, "boero");
    entityManager.flush();

    assertThat(institutionRepository.existsBySlug("boero")).isTrue();
    assertThat(institutionRepository.existsBySlug("other-slug")).isFalse();
  }

  @Test
  @DisplayName("Should detect slug duplicates excluding current institution")
  void existsBySlugAndIdNot_excludesCurrentInstitution() {
    Institution institution = createInstitution(entityManager, "boero");
    entityManager.flush();

    assertThat(institutionRepository.existsBySlugAndIdNot("boero", institution.getId())).isFalse();
    assertThat(institutionRepository.existsBySlugAndIdNot("boero", UUID.randomUUID())).isTrue();
  }

  @Test
  @DisplayName("Should fetch institution with city and province by id")
  void findWithCityAndProvinceById_loadsAssociations() {
    Country countryEntity = persist(entityManager, country("ARG"));
    Province provinceEntity = persist(entityManager, province(countryEntity, "14"));
    City cityEntity = persist(entityManager, city(provinceEntity, "140182"));
    Institution saved = persist(entityManager, institution(cityEntity, "boero-detail"));
    entityManager.flush();
    entityManager.clear();

    Institution found =
        institutionRepository.findWithCityAndProvinceById(saved.getId()).orElseThrow();

    assertThat(found.getCity().getName()).isEqualTo("Villa Maria");
    assertThat(found.getCity().getProvince().getName()).isEqualTo("Cordoba");
  }
}
