package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
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
class ListInstitutionsUseCaseTest {

  @Mock private InstitutionRepository institutionRepository;

  @InjectMocks private ListInstitutionsUseCase listInstitutionsUseCase;

  @Test
  @DisplayName("Should list only active institutions without administrative fields")
  void execute_returnsActiveInstitutionsWithoutAdministrativeData() {
    Institution institution = activeInstitution();
    when(institutionRepository.findByActiveTrue(PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(institution)));

    var response = listInstitutionsUseCase.execute(PageRequest.of(0, 20));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst())
        .satisfies(
            item -> {
              assertThat(item.id()).isEqualTo(institution.getId());
              assertThat(item.name()).isEqualTo("Conservatorio Boero");
              assertThat(item.slug()).isEqualTo("boero-villa-maria");
              assertThat(item.city()).isEqualTo("Villa Maria");
              assertThat(item.province()).isEqualTo("Cordoba");
            });
  }

  private static Institution activeInstitution() {
    Province province = Province.builder().name("Cordoba").build();
    City city = City.builder().name("Villa Maria").province(province).build();

    return Institution.builder()
        .id(UUID.randomUUID())
        .name("Conservatorio Boero")
        .slug("boero-villa-maria")
        .city(city)
        .phoneNumber("0353-123456")
        .email("info@boero.edu.ar")
        .active(true)
        .build();
  }
}
