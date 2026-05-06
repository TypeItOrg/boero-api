package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.City;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {
  Optional<City> findByProvinceIdAndGeorefId(UUID provinceId, String georefId);

  Optional<City> findByGeorefId(String georefId);
}
