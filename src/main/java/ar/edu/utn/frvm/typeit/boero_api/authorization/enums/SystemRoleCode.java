package ar.edu.utn.frvm.typeit.boero_api.authorization.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemRoleCode {
  INSTITUTIONAL_AUTHORITY("Autoridad Institucional"),
  ADMINISTRATIVE("Administrativo"),
  TEACHER("Docente"),
  GUARDIAN("Tutor"),
  APPLICANT("Postulante"),
  STUDENT("Estudiante");

  private final String displayName;
}
