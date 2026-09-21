package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.ScopedResource;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.ScopedResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicAccessGuard {
  private final EntityManager entityManager;
  private final ScopedAuthorizationService authorization;

  @Transactional(readOnly = true)
  public void requireAny(
      Set<PermissionCode> permissions, UUID institutionId, ScopedResource resource, UUID id) {
    for (var permission : permissions) {
      try {
        require(permission, institutionId, resource, id);
        return;
      } catch (ScopedResourceNotFoundException ignored) {
      }
    }
    throw new ScopedResourceNotFoundException();
  }

  @Transactional(readOnly = true)
  public void require(
      PermissionCode permission, UUID institutionId, ScopedResource resource, UUID id) {
    UUID path =
        entityManager
            .createQuery(
                "select "
                    + resource.getTrainingPathExpression()
                    + " from "
                    + resource.getEntity()
                    + " resource where resource.id = :id and "
                    + ((resource == ScopedResource.PREREQUISITE
                            || resource == ScopedResource.ACADEMIC_LEVEL)
                        ? "resource.studyPlan.institution.id"
                        : "resource.institution.id")
                    + " = :institutionId",
                UUID.class)
            .setParameter("id", id)
            .setParameter("institutionId", institutionId)
            .getResultStream()
            .findFirst()
            .orElseThrow(ScopedResourceNotFoundException::new);
    authorization.require(permission, institutionId, path);
  }
}
