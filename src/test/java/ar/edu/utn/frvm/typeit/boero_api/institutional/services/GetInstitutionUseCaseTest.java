package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionInactiveException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetInstitutionUseCaseTest {

  @Mock private InstitutionRepository institutionRepository;

  @InjectMocks private GetInstitutionUseCase getInstitutionUseCase;

  @Test
  @DisplayName("Should return active institution detail")
  void execute_returnsActiveInstitutionDetail() {
    UUID institutionId = UUID.randomUUID();
    Institution institution = institutionWith(institutionId, true);
    when(institutionRepository.findWithCityAndProvinceById(institutionId))
        .thenReturn(Optional.of(institution));

    var response = getInstitutionUseCase.execute(institutionId);

    assertThat(response.id()).isEqualTo(institutionId);
    assertThat(response.name()).isEqualTo("Conservatorio Boero");
    assertThat(response.active()).isTrue();
    assertThat(response.city()).isEqualTo("Villa Maria");
    assertThat(response.province()).isEqualTo("Cordoba");
  }

  @Test
  @DisplayName("Should throw when institution does not exist")
  void execute_throwsWhenInstitutionNotFound() {
    UUID institutionId = UUID.randomUUID();
    when(institutionRepository.findWithCityAndProvinceById(institutionId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> getInstitutionUseCase.execute(institutionId))
        .isInstanceOf(InstitutionNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw when institution is inactive")
  void execute_throwsWhenInstitutionInactive() {
    UUID institutionId = UUID.randomUUID();
    when(institutionRepository.findWithCityAndProvinceById(institutionId))
        .thenReturn(Optional.of(institutionWith(institutionId, false)));

    assertThatThrownBy(() -> getInstitutionUseCase.execute(institutionId))
        .isInstanceOf(InstitutionInactiveException.class);
  }

  private static Institution institutionWith(UUID id, boolean active) {
    Province province = Province.builder().name("Cordoba").build();
    City city = City.builder().name("Villa Maria").province(province).build();

    return Institution.builder()
        .id(id)
        .name("Conservatorio Boero")
        .slug("boero-villa-maria")
        .city(city)
        .active(active)
        .build();
  }
}
