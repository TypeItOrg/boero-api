package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsibleDto {
  private String fullName;
  private String documentNumber;
  private String occupation;
  private String phoneNumber;
  private String email;
  private String educationLevel;
}
