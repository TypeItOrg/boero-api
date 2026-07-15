package ar.edu.utn.frvm.typeit.boero_api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinimumAgeValidatorTest {

  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void acceptsExactMinimumAge() {
    final LocalDate birthDate = LocalDate.now(ARGENTINA_TIME_ZONE).minusYears(3);

    assertThat(validator.validate(new BirthDateValue(birthDate))).isEmpty();
  }

  @Test
  void rejectsOneDayBeforeMinimumAge() {
    final LocalDate birthDate = LocalDate.now(ARGENTINA_TIME_ZONE).minusYears(3).plusDays(1);

    assertThat(validator.validate(new BirthDateValue(birthDate)))
        .singleElement()
        .satisfies(
            violation ->
                assertThat(violation.getMessage())
                    .isEqualTo("La persona debe tener al menos 3 años."));
  }

  private record BirthDateValue(@MinimumAge(3) LocalDate birthDate) {}
}
