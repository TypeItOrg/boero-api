package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateAddressRequest(
    @NotNull(message = ValidationMessages.CITY_REQUIRED) UUID cityId,
    @NotBlank(message = ValidationMessages.STREET_REQUIRED) String street,
    @Size(max = 50, message = ValidationMessages.NUMBER_MAX_LENGTH) String number,
    @Size(max = 50, message = ValidationMessages.FLOOR_MAX_LENGTH) String floor,
    @Size(max = 50, message = ValidationMessages.APARTMENT_MAX_LENGTH) String apartment,
    String neighborhood,
    String additionalInfo) {}
