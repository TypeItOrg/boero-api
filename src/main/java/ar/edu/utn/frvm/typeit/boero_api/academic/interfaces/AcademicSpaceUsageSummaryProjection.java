package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

public interface AcademicSpaceUsageSummaryProjection {
  long getTotalPlans();

  long getActivePlans();

  long getDraftPlans();

  long getInactivePlans();

  long getTotalPlacements();

  long getUnassignedPlacements();
}
