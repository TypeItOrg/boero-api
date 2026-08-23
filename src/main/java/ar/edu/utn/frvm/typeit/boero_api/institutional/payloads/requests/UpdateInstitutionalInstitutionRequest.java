package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests;

import ar.edu.utn.frvm.typeit.boero_api.common.validation.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateInstitutionalInstitutionRequest(
    @NotBlank(message = ValidationMessages.NAME_REQUIRED)
        @Size(max = 255, message = ValidationMessages.NAME_MAX_LENGTH)
        String name,
    @NotNull(message = ValidationMessages.CITY_REQUIRED) UUID cityId,
    String street,
    @Size(max = 50, message = ValidationMessages.NUMBER_MAX_LENGTH) String number,
    String neighborhood,
    String additionalInfo,
    @Size(max = 30, message = ValidationMessages.PHONE_MAX_LENGTH) String phoneNumber,
    @Email(message = ValidationMessages.EMAIL_FORMAT)
        @Size(max = 150, message = ValidationMessages.EMAIL_MAX_LENGTH)
        String email) {}
