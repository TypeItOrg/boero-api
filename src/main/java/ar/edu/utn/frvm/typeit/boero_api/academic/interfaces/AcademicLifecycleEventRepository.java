package ar.edu.utn.frvm.typeit.boero_api.academic.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLifecycleEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicLifecycleEventRepository
    extends JpaRepository<AcademicLifecycleEvent, UUID> {}
