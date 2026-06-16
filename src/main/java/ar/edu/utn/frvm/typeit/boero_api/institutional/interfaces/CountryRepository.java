package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Country;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CountryRepository extends JpaRepository<Country, UUID> {
  Optional<Country> findByIsoCode(String isoCode);

  @Query(
      """
      SELECT c FROM Country c
      WHERE LOWER(FUNCTION('TRANSLATE', c.name, 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
            LIKE LOWER(FUNCTION('TRANSLATE', CONCAT('%', :search, '%'), 'áéíóúÁÉÍÓÚüÜñÑ', 'aeiouAEIOUuUnN'))
      """)
  Page<Country> searchByName(@Param("search") String search, Pageable pageable);
}
