package ar.edu.utn.frvm.typeit.boero_api.institutional.payloads;

import lombok.Builder;

@Builder
public record PlatformDashboardSummaryResponse(
    long institutions,
    long activeInstitutions,
    long inactiveInstitutions,
    long people,
    long usersWithAccess) {}
