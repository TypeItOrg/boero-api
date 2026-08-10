package ar.edu.utn.frvm.typeit.boero_api.academic.services;

import ar.edu.utn.frvm.typeit.boero_api.academic.exceptions.AcademicLevelNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.academic.interfaces.AcademicLevelRepository;
import ar.edu.utn.frvm.typeit.boero_api.academic.payloads.AcademicLevelResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAcademicLevelUseCase {
  private final AcademicLevelRepository academicLevelRepository;

  @Transactional(readOnly = true)
  public AcademicLevelResponse execute(final UUID institutionId, final UUID id) {
    return academicLevelRepository
        .findByIdAndStudyPlan_Institution_Id(id, institutionId)
        .map(AcademicLevelResponse::from)
        .orElseThrow(AcademicLevelNotFoundException::new);
  }
}
