package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDataDto {
  private String firstName;
  private String lastName;
  private String documentNumber;
  private LocalDate birthDate;
  private String phoneNumber;
  private String email;
}
