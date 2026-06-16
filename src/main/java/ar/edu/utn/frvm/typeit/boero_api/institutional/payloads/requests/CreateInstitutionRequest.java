package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.validation.InstitutionFieldConstraints.SLUG_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.institutional.validation.InstitutionFieldConstraints.SLUG_PATTERN;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateInstitutionRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size(max = 255, message = "El nombre debe tener menos de 255 caracteres.")
        String name,
    @NotBlank(message = "El slug es requerido.")
        @Size(max = SLUG_MAX, message = "El slug debe tener menos de 100 caracteres.")
        @Pattern(
            regexp = SLUG_PATTERN,
            message = "El slug solo puede contener letras minúsculas, números y guiones.")
        String slug,
    @NotNull(message = "La ciudad es requerida.") UUID cityId,
    String street,
    @Size(max = 50, message = "El número debe tener menos de 50 caracteres.") String number,
    String neighborhood,
    String additionalInfo,
    @Size(max = 30, message = "El teléfono debe tener menos de 30 caracteres.") String phoneNumber,
    @Email(message = "El correo electrónico debe tener un formato válido.")
        @Size(max = 150, message = "El correo electrónico debe tener menos de 150 caracteres.")
        String email) {}
