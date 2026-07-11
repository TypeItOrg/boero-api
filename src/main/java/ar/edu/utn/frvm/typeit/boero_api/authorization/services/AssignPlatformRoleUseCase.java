package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformAccountRole;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPlatformRoleUseCase {

  private final RoleRepository roleRepository;
  private final PlatformAccountRoleRepository platformAccountRoleRepository;
  private final SessionRevocationService sessionRevocationService;

  @org.springframework.cache.annotation.Caching(
      evict = {
        @org.springframework.cache.annotation.CacheEvict(
            value = "platformAccountPermissions",
            key = "#account.id"),
        @org.springframework.cache.annotation.CacheEvict(
            value = "platformAccountRoles",
            key = "#account.id")
      })
  @Transactional
  public void execute(PlatformAccount account, PlatformRoleCode roleCode) {
    execute(account, roleCode, false);
  }

  @org.springframework.cache.annotation.Caching(
      evict = {
        @org.springframework.cache.annotation.CacheEvict(
            value = "platformAccountPermissions",
            key = "#account.id"),
        @org.springframework.cache.annotation.CacheEvict(
            value = "platformAccountRoles",
            key = "#account.id")
      })
  @Transactional
  public void execute(PlatformAccount account, PlatformRoleCode roleCode, boolean revokeSessions) {
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(RoleScope.PLATFORM, roleCode.name())
            .orElseThrow(
                () -> new IllegalStateException("Platform role not seeded: " + roleCode.name()));

    if (platformAccountRoleRepository.existsByPlatformAccount_IdAndRole_Id(
        account.getId(), role.getId())) {
      return;
    }

    platformAccountRoleRepository.save(
        PlatformAccountRole.builder().platformAccount(account).role(role).build());

    if (revokeSessions) {
      sessionRevocationService.revokePlatformAccountSessions(account.getId());
    }
  }
}
