package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Student;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, UUID> {

  boolean existsByInstitution_IdAndPerson_Id(UUID institutionId, UUID personId);

  long countByInstitution_Id(UUID institutionId);

  @Query(value = "SELECT nextval('student_file_number_seq')", nativeQuery = true)
  long nextFileNumberSequenceValue();
}
