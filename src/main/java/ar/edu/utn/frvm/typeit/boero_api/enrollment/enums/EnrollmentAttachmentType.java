package ar.edu.utn.frvm.typeit.boero_api.enrollment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentAttachmentType {
  DNI_FRONT("Frente del DNI"),
  DNI_BACK("Dorso del DNI"),
  SECONDARY_CERTIFICATE("Certificado de estudios secundarios"),
  HEALTH_REPORT("Ficha o certificado médico"),
  PHOTO_ID("Foto carnet");

  private final String displayName;
}
