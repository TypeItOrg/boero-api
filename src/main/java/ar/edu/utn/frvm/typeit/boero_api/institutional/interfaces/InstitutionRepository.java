package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findBySlug(String slug);

  @EntityGraph(attributePaths = {"city", "city.province"})
  Page<Institution> findByActiveTrue(Pageable pageable);

  @EntityGraph(attributePaths = {"city", "city.province", "city.province.country"})
  @Query("select institution from Institution institution")
  Page<Institution> findAllWithLocation(Pageable pageable);

  @EntityGraph(attributePaths = {"city", "city.province", "city.province.country"})
  @Query(
      """
      SELECT institution FROM Institution institution
      JOIN institution.city city
      JOIN city.province province
      JOIN province.country country
      WHERE (:active IS NULL OR institution.active = :active)
        AND (
          :search IS NULL
          OR UNACCENT_LOWER(institution.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(institution.slug) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(city.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(province.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR UNACCENT_LOWER(country.name) LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
      """)
  Page<Institution> findWithLocationByFilters(
      @Param("search") String search, @Param("active") Boolean active, Pageable pageable);

  @EntityGraph(attributePaths = {"city", "city.province"})
  Optional<Institution> findByIdAndActiveTrue(UUID id);

  @EntityGraph(attributePaths = {"city", "city.province", "city.province.country"})
  Optional<Institution> findWithLocationById(UUID id);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  long countByActiveTrue();

  @Query(
      """
      SELECT YEAR(institution.createdAt) AS year,
             MONTH(institution.createdAt) AS month,
             COUNT(institution) AS institutionCount
      FROM Institution institution
      WHERE institution.createdAt >= :from AND institution.createdAt < :until
      GROUP BY YEAR(institution.createdAt), MONTH(institution.createdAt)
      ORDER BY YEAR(institution.createdAt), MONTH(institution.createdAt)
      """)
  List<MonthlyInstitutionCount> countCreatedByMonth(
      @Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

  @EntityGraph(attributePaths = {"city", "city.province"})
  List<Institution> findTop5ByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT institution FROM Institution institution WHERE institution.id = :id")
  Optional<Institution> findByIdForUpdate(@Param("id") UUID id);
}
