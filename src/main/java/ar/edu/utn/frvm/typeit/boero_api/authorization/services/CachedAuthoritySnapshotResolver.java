package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.cache.AuthorizationCacheNames;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRoleRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CachedAuthoritySnapshotResolver {

  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final PlatformAccountRoleRepository platformAccountRoleRepository;

  @Cacheable(
      value = AuthorizationCacheNames.PERSON_AUTHORITIES,
      key = "#personId + '-' + #institutionId")
  @Transactional(readOnly = true)
  public InstitutionalAuthoritySnapshot resolveForPerson(UUID personId, UUID institutionId) {
    List<PersonRoleAssignmentRepository.AuthorityRow> rows =
        personRoleAssignmentRepository.findAuthoritiesByPersonIdAndInstitutionId(
            personId, institutionId);

    Set<PermissionCode> permissions =
        rows.stream()
            .map(PersonRoleAssignmentRepository.AuthorityRow::getPermissionCode)
            .filter(code -> code != null)
            .map(PermissionCode::fromCode)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(PermissionCode.class)));
    List<String> roles =
        rows.stream()
            .map(PersonRoleAssignmentRepository.AuthorityRow::getRoleName)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

    return new InstitutionalAuthoritySnapshot(permissions, roles);
  }

  @Cacheable(value = AuthorizationCacheNames.PLATFORM_AUTHORITIES, key = "#platformAccountId")
  @Transactional(readOnly = true)
  public PlatformAuthoritySnapshot resolveForPlatformAccount(UUID platformAccountId) {
    List<PlatformAccountRoleRepository.AuthorityRow> rows =
        platformAccountRoleRepository.findAuthoritiesByPlatformAccountId(platformAccountId);

    Set<PermissionCode> permissions =
        rows.stream()
            .map(PlatformAccountRoleRepository.AuthorityRow::getPermissionCode)
            .filter(code -> code != null)
            .map(PermissionCode::fromCode)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(PermissionCode.class)));
    Set<PlatformRoleCode> roles =
        rows.stream()
            .map(PlatformAccountRoleRepository.AuthorityRow::getRoleCode)
            .map(this::parsePlatformRoleCode)
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableSet());

    return new PlatformAuthoritySnapshot(permissions, roles);
  }

  private Optional<PlatformRoleCode> parsePlatformRoleCode(String code) {
    try {
      return Optional.of(PlatformRoleCode.valueOf(code));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
