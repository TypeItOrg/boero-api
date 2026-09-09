package ar.edu.utn.frvm.typeit.boero_api.common.validation;

import static ar.edu.utn.frvm.typeit.boero_api.common.time.BusinessDateProvider.BUSINESS_ZONE;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {

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
        LocalDate.now(context.getClockProvider().getClock().withZone(BUSINESS_ZONE))
            .minusYears(minimumAge);
    return !value.isAfter(latestAllowedBirthDate);
  }
}
