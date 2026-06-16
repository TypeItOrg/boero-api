package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.ProvinceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.ProvinceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ListCitiesUseCaseTest {

  @Mock private CityRepository cityRepository;
  @Mock private ProvinceRepository provinceRepository;

  @InjectMocks private ListCitiesUseCase listCitiesUseCase;

  @Test
  @DisplayName("Should list all cities when no search provided")
  void execute_returnsAllCitiesWithoutSearch() {
    Province province = provinceWith("Cordoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa Maria").province(province).build();
    when(cityRepository.findAll(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.execute(null, PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst())
        .satisfies(
            item -> {
              assertThat(item.name()).isEqualTo("Villa Maria");
              assertThat(item.province()).isEqualTo("Cordoba");
              assertThat(item.provinceId()).isEqualTo(province.getId());
            });
  }

  @Test
  @DisplayName("Should search cities by name across all provinces")
  void execute_searchesByCityOrProvinceName() {
    Province province = provinceWith("Cordoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa Maria").province(province).build();
    when(cityRepository.searchByNameOrProvince("maria", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.execute("maria", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Villa Maria");
  }

  @Test
  @DisplayName("Should search by province when search matches province name")
  void execute_searchesByProvinceName() {
    Province province = provinceWith("Cordoba");
    City city = City.builder().id(UUID.randomUUID()).name("La Floresta").province(province).build();
    when(cityRepository.searchByNameOrProvince("cordoba", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.execute("cordoba", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().province()).isEqualTo("Cordoba");
  }

  @Test
  @DisplayName("Should match city with accent using search without accent")
  void execute_matchesAccentedCityWithUnaccentedSearch() {
    Province province = provinceWith("Córdoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa María").province(province).build();
    when(cityRepository.searchByNameOrProvince("maria", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.execute("maria", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Villa María");
  }

  @Test
  @DisplayName("Should match province with accent using search without accent")
  void execute_matchesAccentedProvinceWithUnaccentedSearch() {
    Province province = provinceWith("Córdoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa María").province(province).build();
    when(cityRepository.searchByNameOrProvince("cordoba", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.execute("cordoba", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().province()).isEqualTo("Córdoba");
  }

  @Test
  @DisplayName("Should return empty when search does not match")
  void execute_returnsEmptyForNoMatches() {
    when(cityRepository.searchByNameOrProvince("xyzabc", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of()));

    var response = listCitiesUseCase.execute("xyzabc", PageRequest.of(0, 20));

    assertThat(response.items()).isEmpty();
  }

  @Test
  @DisplayName("Should list cities by province without search")
  void executeByProvince_returnsCitiesForProvince() {
    UUID provinceId = UUID.randomUUID();
    Province province = provinceWith("Cordoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa Maria").province(province).build();
    when(provinceRepository.existsById(provinceId)).thenReturn(true);
    when(cityRepository.findByProvinceId(provinceId, PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.executeByProvince(provinceId, null, PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Villa Maria");
    verify(cityRepository, never()).searchByNameOrProvince(any(), any());
  }

  @Test
  @DisplayName("Should search cities within a province")
  void executeByProvince_withSearch() {
    UUID provinceId = UUID.randomUUID();
    Province province = provinceWith("Cordoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa Maria").province(province).build();
    when(provinceRepository.existsById(provinceId)).thenReturn(true);
    when(cityRepository.searchByProvinceAndName(provinceId, "maria", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.executeByProvince(provinceId, "maria", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Villa Maria");
  }

  @Test
  @DisplayName("Should match accented city within province using unaccented search")
  void executeByProvince_matchesAccentedCityWithUnaccentedSearch() {
    UUID provinceId = UUID.randomUUID();
    Province province = provinceWith("Córdoba");
    City city = City.builder().id(UUID.randomUUID()).name("Villa María").province(province).build();
    when(provinceRepository.existsById(provinceId)).thenReturn(true);
    when(cityRepository.searchByProvinceAndName(provinceId, "maria", PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(city)));

    var response = listCitiesUseCase.executeByProvince(provinceId, "maria", PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Villa María");
  }

  @Test
  @DisplayName("Should throw when province does not exist")
  void executeByProvince_throwsWhenProvinceNotFound() {
    UUID unknownId = UUID.randomUUID();
    when(provinceRepository.existsById(unknownId)).thenReturn(false);

    assertThatThrownBy(
            () -> listCitiesUseCase.executeByProvince(unknownId, null, PageRequest.of(0, 20)))
        .isInstanceOf(ProvinceNotFoundException.class);
  }

  private static Province provinceWith(String name) {
    return Province.builder().id(UUID.randomUUID()).name(name).build();
  }
}
