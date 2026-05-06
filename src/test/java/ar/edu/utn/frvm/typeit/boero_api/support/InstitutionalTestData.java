package ar.edu.utn.frvm.typeit.boero_api.support;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Country;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import jakarta.persistence.EntityManager;
import java.util.UUID;

public final class InstitutionalTestData {

  private InstitutionalTestData() {}

  public static Country country(String isoCode) {
    return Country.builder().name("Country " + isoCode).isoCode(isoCode).build();
  }

  public static Province province(Country country, String georefId) {
    return Province.builder().country(country).name("Cordoba").georefId(georefId).build();
  }

  public static City city(Province province, String georefId) {
    return City.builder()
        .province(province)
        .name("Villa Maria")
        .georefId(georefId)
        .departmentGeorefId("14098")
        .departmentName("General San Martin")
        .municipalityGeorefId("140182")
        .municipalityName("Villa Maria")
        .build();
  }

  public static Institution institution(City city, String slug) {
    return Institution.builder().city(city).name("Conservatorio Boero").slug(slug).build();
  }

  public static Person person(Institution institution, String documentNumber) {
    return Person.builder()
        .institution(institution)
        .firstName("Ana")
        .lastName("Garcia")
        .documentNumber(documentNumber)
        .build();
  }

  public static User user(Institution institution, Person person) {
    return User.builder().institution(institution).person(person).password("encoded-password").build();
  }

  public static Institution createInstitution(EntityManager entityManager, String slug) {
    Country country = persist(entityManager, country(isoCode()));
    Province province = persist(entityManager, province(country, suffix()));
    City city = persist(entityManager, city(province, suffix()));
    return persist(entityManager, institution(city, slug));
  }

  public static User createUser(
      EntityManager entityManager, Institution institution, String documentNumber) {
    Person person = persist(entityManager, person(institution, documentNumber));
    return persist(entityManager, user(institution, person));
  }

  public static <T> T persist(EntityManager entityManager, T entity) {
    entityManager.persist(entity);
    return entity;
  }

  private static String suffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private static String isoCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 3).toUpperCase();
  }
}
