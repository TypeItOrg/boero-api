package ar.edu.utn.frvm.typeit.boero_api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneId;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {

  private static final ZoneId ARGENTINA_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private int minimumAge;

  @Override
  public void initialize(final MinimumAge constraintAnnotation) {
    minimumAge = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(final LocalDate value, final ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    final LocalDate latestAllowedBirthDate =
        LocalDate.now(ARGENTINA_TIME_ZONE).minusYears(minimumAge);
    return !value.isAfter(latestAllowedBirthDate);
  }
}
