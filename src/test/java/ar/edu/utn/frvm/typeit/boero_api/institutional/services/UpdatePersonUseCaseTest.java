package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Address;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Country;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CountryNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.AddressRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CountryRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdateAddressRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdatePersonUseCaseTest {

  @Mock private PersonRepository personRepository;
  @Mock private CityRepository cityRepository;
  @Mock private CountryRepository countryRepository;
  @Mock private AddressRepository addressRepository;

  @Spy private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @InjectMocks private UpdatePersonUseCase updatePersonUseCase;

  private UUID institutionId;
  private UUID personId;
  private Institution institution;
  private Person person;
  private JwtAuthenticatedUser principal;

  @BeforeEach
  void setUp() {
    institutionId = UUID.randomUUID();
    personId = UUID.randomUUID();
    institution = Institution.builder().id(institutionId).name("Conservatorio Boero").build();
    person =
        Person.builder()
            .id(personId)
            .firstName("Juan")
            .lastName("Pérez")
            .documentNumber("12345678")
            .institution(institution)
            .build();
    principal =
        JwtAuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .personId(personId)
            .institutionId(institutionId)
            .build();

    when(personRepository.findByIdAndInstitution_Id(personId, institutionId))
        .thenReturn(Optional.of(person));
    when(personRepository.findWithDetailsByIdAndInstitution_Id(personId, institutionId))
        .thenReturn(Optional.of(person));
  }

  @Test
  @DisplayName("Should update first name")
  void execute_updatesFirstName() {
    UpdatePersonRequest request =
        new UpdatePersonRequest("Carlos", null, null, null, null, null, null, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.firstName()).isEqualTo("Carlos");
    verify(personRepository).save(any(Person.class));
  }

  @Test
  @DisplayName("Should update email")
  void execute_updatesEmail() {
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, "carlos@test.com", null, null, null, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.email()).isEqualTo("carlos@test.com");
  }

  @Test
  @DisplayName("Should update birth city")
  void execute_updatesBirthCity() {
    UUID cityId = UUID.randomUUID();
    Province province = Province.builder().name("Córdoba").build();
    City city = City.builder().id(cityId).name("Villa María").province(province).build();
    when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, null, null, cityId, null, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.birthCity()).isNotNull();
    assertThat(response.birthCity().name()).isEqualTo("Villa María");
  }

  @Test
  @DisplayName("Should update nationality country")
  void execute_updatesNationalityCountry() {
    UUID countryId = UUID.randomUUID();
    Country country = Country.builder().id(countryId).name("Argentina").isoCode("ARG").build();
    when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, null, null, null, countryId, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.nationalityCountry()).isNotNull();
    assertThat(response.nationalityCountry().name()).isEqualTo("Argentina");
  }

  @Test
  @DisplayName("Should create new address when person has none")
  void execute_createsAddress() {
    UUID cityId = UUID.randomUUID();
    Province province = Province.builder().name("Córdoba").build();
    City city = City.builder().id(cityId).name("Villa María").province(province).build();
    when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
    UpdatePersonRequest request =
        new UpdatePersonRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new UpdateAddressRequest(cityId, "San Martín", "123", null, null, null, null));

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.address()).isNotNull();
    assertThat(response.address().street()).isEqualTo("San Martín");

    ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
    verify(addressRepository).save(captor.capture());
    assertThat(captor.getValue().getInstitution()).isEqualTo(institution);
  }

  @Test
  @DisplayName("Should update existing address")
  void execute_updatesExistingAddress() {
    UUID cityId = UUID.randomUUID();
    Province province = Province.builder().name("Córdoba").build();
    City newCity = City.builder().id(cityId).name("Córdoba").province(province).build();
    City oldCity = City.builder().name("Villa María").province(province).build();
    Address address =
        Address.builder()
            .id(UUID.randomUUID())
            .institution(institution)
            .street("San Martín")
            .number("100")
            .city(oldCity)
            .build();
    person.changeAddress(address);
    when(cityRepository.findById(cityId)).thenReturn(Optional.of(newCity));
    UpdatePersonRequest request =
        new UpdatePersonRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new UpdateAddressRequest(cityId, "Belgrano", "200", null, "3B", null, null));

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.address().street()).isEqualTo("Belgrano");
    assertThat(response.address().number()).isEqualTo("200");
    assertThat(response.address().apartment()).isEqualTo("3B");
    assertThat(response.address().city().name()).isEqualTo("Córdoba");
  }

  @Test
  @DisplayName("Should update birth date")
  void execute_updatesBirthDate() {
    LocalDate birthDate = LocalDate.of(1990, 5, 15);
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, birthDate, null, null, null, null, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.birthDate()).isEqualTo(birthDate);
  }

  @Test
  @DisplayName("Should only update provided fields (partial update)")
  void execute_partialUpdate() {
    person.updateContact(person.getEmail(), "0353-123456");
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, "García", null, null, null, null, null, null);

    var response = updatePersonUseCase.execute(principal, request);

    assertThat(response.firstName()).isEqualTo("Juan");
    assertThat(response.lastName()).isEqualTo("García");
    assertThat(response.phoneNumber()).isEqualTo("0353-123456");
  }

  @Test
  @DisplayName("Should throw when person not found")
  void execute_throwsWhenPersonNotFound() {
    when(personRepository.findByIdAndInstitution_Id(personId, institutionId))
        .thenReturn(Optional.empty());
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> updatePersonUseCase.execute(principal, request))
        .isInstanceOf(PersonNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw when birth city not found")
  void execute_throwsWhenBirthCityNotFound() {
    UUID cityId = UUID.randomUUID();
    when(cityRepository.findById(cityId)).thenReturn(Optional.empty());
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, null, null, cityId, null, null);

    assertThatThrownBy(() -> updatePersonUseCase.execute(principal, request))
        .isInstanceOf(CityNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw when nationality country not found")
  void execute_throwsWhenCountryNotFound() {
    UUID countryId = UUID.randomUUID();
    when(countryRepository.findById(countryId)).thenReturn(Optional.empty());
    UpdatePersonRequest request =
        new UpdatePersonRequest(null, null, null, null, null, null, countryId, null);

    assertThatThrownBy(() -> updatePersonUseCase.execute(principal, request))
        .isInstanceOf(CountryNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw when address city not found")
  void execute_throwsWhenAddressCityNotFound() {
    UUID cityId = UUID.randomUUID();
    when(cityRepository.findById(cityId)).thenReturn(Optional.empty());
    UpdatePersonRequest request =
        new UpdatePersonRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new UpdateAddressRequest(cityId, "San Martín", "123", null, null, null, null));

    assertThatThrownBy(() -> updatePersonUseCase.execute(principal, request))
        .isInstanceOf(CityNotFoundException.class);
  }

  @Test
  @DisplayName("Should validate person after update")
  void execute_validatesPersonAfterUpdate() {
    UpdatePersonRequest request =
        new UpdatePersonRequest("A", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> updatePersonUseCase.execute(principal, request))
        .isInstanceOf(ConstraintViolationException.class);

    verify(personRepository, never()).save(any());
  }
}
