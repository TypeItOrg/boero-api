package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findBySlug(String slug);

  @EntityGraph(attributePaths = {"city", "city.province"})
  Page<Institution> findByActiveTrue(Pageable pageable);

  @EntityGraph(attributePaths = {"city", "city.province"})
  Optional<Institution> findByIdAndActiveTrue(UUID id);

  @EntityGraph(attributePaths = {"city", "city.province"})
  Optional<Institution> findWithCityAndProvinceById(UUID id);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);
}
