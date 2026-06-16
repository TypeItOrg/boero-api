package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformAccount;
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
public class RevokePlatformRoleUseCase {

  private final RoleRepository roleRepository;
  private final PlatformAccountRoleRepository platformAccountRoleRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(PlatformAccount account, PlatformRoleCode roleCode) {
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(RoleScope.PLATFORM, roleCode.name())
            .orElseThrow(
                () -> new IllegalStateException("Platform role not seeded: " + roleCode.name()));

    platformAccountRoleRepository.findByPlatformAccount_Id(account.getId()).stream()
        .filter(assignment -> assignment.getRole().getId().equals(role.getId()))
        .findFirst()
        .ifPresent(
            assignment -> {
              platformAccountRoleRepository.delete(assignment);
              sessionRevocationService.revokePlatformAccountSessions(account.getId());
            });
  }
}
