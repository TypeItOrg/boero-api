package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateAddressRequest(
    @NotNull(message = "La ciudad es requerida.") UUID cityId,
    @NotBlank(message = "La calle es requerida.") String street,
    @Size(max = 50, message = "El número debe tener menos de 50 caracteres.") String number,
    @Size(max = 50, message = "El piso debe tener menos de 50 caracteres.") String floor,
    @Size(max = 50, message = "El departamento debe tener menos de 50 caracteres.")
        String apartment,
    String neighborhood,
    String additionalInfo) {}
