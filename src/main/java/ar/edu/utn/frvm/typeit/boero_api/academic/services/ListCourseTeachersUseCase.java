package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.TeacherOptionResponse;
import ar.edu.utn.frvm.typeit.boero_api.academic.validation.AcademicNameNormalizer;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCourseTeachersUseCase {
  private final RoleRepository roleRepository;
  private final PersonRepository personRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<TeacherOptionResponse> execute(
      final UUID institutionId, final String search, final Pageable pageable) {
    final var localRole =
        roleRepository.findByScopeAndCodeAndInstitution_Id(
            RoleScope.INSTITUTION, SystemRoleCode.TEACHER.name(), institutionId);
    final var teacherRoleId =
        localRole
            .map(role -> role.getId())
            .orElseGet(
                () ->
                    roleRepository
                        .findByScopeAndCodeAndInstitutionIsNull(
                            RoleScope.INSTITUTION, SystemRoleCode.TEACHER.name())
                        .map(role -> role.getId())
                        .orElse(null));
    if (teacherRoleId == null) {
      return PaginatedResponse.from(org.springframework.data.domain.Page.empty());
    }
    return PaginatedResponse.from(
        personRepository
            .search(institutionId, AcademicNameNormalizer.search(search), teacherRoleId, pageable)
            .map(TeacherOptionResponse::from));
  }
}
