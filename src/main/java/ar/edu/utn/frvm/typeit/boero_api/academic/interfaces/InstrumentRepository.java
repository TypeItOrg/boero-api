package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
  @Query(
      """
      SELECT instrument FROM Instrument instrument
      WHERE instrument.institution.id = :institutionId
        AND (:active IS NULL OR instrument.active = :active)
        AND (:search IS NULL OR UNACCENT_LOWER(instrument.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%')))
      """)
  Page<Instrument> findByFilters(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      @Param("active") Boolean active,
      Pageable pageable);

  Optional<Instrument> findByIdAndInstitution_Id(UUID id, UUID institutionId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM instruments WHERE institution_id = :institutionId AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedName(
      @Param("institutionId") UUID institutionId, @Param("name") String name);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM instruments WHERE institution_id = :institutionId AND instrument_id <> :id AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) = lower(translate(:name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))) ",
      nativeQuery = true)
  boolean existsByNormalizedNameAndIdNot(
      @Param("institutionId") UUID institutionId, @Param("name") String name, @Param("id") UUID id);
}
