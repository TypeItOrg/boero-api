package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.MonthlyInstitutionCount;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.MonthlyInstitutionRegistrationResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.PlatformDashboardResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.PlatformDashboardSummaryResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.RecentInstitutionResponse;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPlatformDashboardUseCase {

  private static final int TREND_MONTHS = 12;

  private final InstitutionRepository institutionRepository;
  private final PersonRepository personRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public PlatformDashboardResponse execute() {
    return execute(YearMonth.now());
  }

  PlatformDashboardResponse execute(final YearMonth currentMonth) {
    final long institutions = institutionRepository.count();
    final long activeInstitutions = institutionRepository.countByActiveTrue();
    final YearMonth firstMonth = currentMonth.minusMonths(TREND_MONTHS - 1L);
    final Map<YearMonth, Long> registrationsByMonth =
        institutionRepository
            .countCreatedByMonth(
                firstMonth.atDay(1).atStartOfDay(),
                currentMonth.plusMonths(1).atDay(1).atStartOfDay())
            .stream()
            .collect(
                Collectors.toMap(
                    row -> YearMonth.of(row.getYear(), row.getMonth()),
                    MonthlyInstitutionCount::getInstitutionCount));

    return PlatformDashboardResponse.builder()
        .summary(
            PlatformDashboardSummaryResponse.builder()
                .institutions(institutions)
                .activeInstitutions(activeInstitutions)
                .inactiveInstitutions(institutions - activeInstitutions)
                .people(personRepository.countByDeletedFalse())
                .usersWithAccess(userRepository.countUsersWithAccess())
                .build())
        .institutionRegistrations(
            IntStream.range(0, TREND_MONTHS)
                .mapToObj(firstMonth::plusMonths)
                .map(
                    month ->
                        MonthlyInstitutionRegistrationResponse.builder()
                            .year(month.getYear())
                            .month(month.getMonthValue())
                            .count(registrationsByMonth.getOrDefault(month, 0L))
                            .build())
                .toList())
        .recentInstitutions(
            institutionRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(RecentInstitutionResponse::from)
                .toList())
        .build();
  }
}
