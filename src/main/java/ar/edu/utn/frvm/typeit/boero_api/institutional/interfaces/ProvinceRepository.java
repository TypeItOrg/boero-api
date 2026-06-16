package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Province;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProvinceRepository extends JpaRepository<Province, UUID> {
  Optional<Province> findByCountryIdAndGeorefId(UUID countryId, String georefId);

  @Query(
      """
      SELECT p FROM Province p
      WHERE LOWER(FUNCTION('TRANSLATE', p.name, 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
            LIKE LOWER(FUNCTION('TRANSLATE', CONCAT('%', :search, '%'), 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
      """)
  Page<Province> searchByName(@Param("search") String search, Pageable pageable);

  Page<Province> findByCountryId(UUID countryId, Pageable pageable);

  @Query(
      """
      SELECT p FROM Province p
      WHERE p.country.id = :countryId
        AND LOWER(FUNCTION('TRANSLATE', p.name, 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
            LIKE LOWER(FUNCTION('TRANSLATE', CONCAT('%', :search, '%'), 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
      """)
  Page<Province> searchByCountryIdAndName(
      @Param("countryId") UUID countryId, @Param("search") String search, Pageable pageable);
}
