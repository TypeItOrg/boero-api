package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Address;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CountryNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.AddressRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CountryRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.UpdatePersonRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePersonUseCase {

  private final PersonRepository personRepository;
  private final CityRepository cityRepository;
  private final CountryRepository countryRepository;
  private final AddressRepository addressRepository;
  private final Validator validator;

  @Transactional
  public PersonResponse execute(JwtAuthenticatedUser principal, UpdatePersonRequest request) {
    Person person =
        personRepository
            .findByIdAndInstitution_Id(principal.personId(), principal.institutionId())
            .orElseThrow(PersonNotFoundException::new);

    if (request.firstName() != null) {
      person.setFirstName(request.firstName());
    }
    if (request.lastName() != null) {
      person.setLastName(request.lastName());
    }
    if (request.birthDate() != null) {
      person.setBirthDate(request.birthDate());
    }
    if (request.email() != null) {
      person.setEmail(request.email());
    }
    if (request.phoneNumber() != null) {
      person.setPhoneNumber(request.phoneNumber());
    }

    if (request.birthCityId() != null) {
      var birthCity =
          cityRepository.findById(request.birthCityId()).orElseThrow(CityNotFoundException::new);
      person.setBirthCity(birthCity);
    }
    if (request.nationalityCountryId() != null) {
      var country =
          countryRepository
              .findById(request.nationalityCountryId())
              .orElseThrow(CountryNotFoundException::new);
      person.setNationalityCountry(country);
    }

    if (request.address() != null) {
      var addressRequest = request.address();
      Address address = person.getAddress();
      if (address == null) {
        address = new Address();
        address.setInstitution(person.getInstitution());
      }

      var city =
          cityRepository.findById(addressRequest.cityId()).orElseThrow(CityNotFoundException::new);
      address.setCity(city);
      address.setStreet(addressRequest.street());
      address.setNumber(addressRequest.number());
      address.setFloor(addressRequest.floor());
      address.setApartment(addressRequest.apartment());
      address.setNeighborhood(addressRequest.neighborhood());
      address.setAdditionalInfo(addressRequest.additionalInfo());

      addressRepository.save(address);
      person.setAddress(address);
    }

    assertPersonValid(person);
    personRepository.save(person);

    return personRepository
        .findWithDetailsByIdAndInstitution_Id(principal.personId(), principal.institutionId())
        .map(PersonResponse::from)
        .orElseThrow(PersonNotFoundException::new);
  }

  private void assertPersonValid(Person person) {
    Set<ConstraintViolation<Person>> violations = validator.validate(person);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
