package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoRepository extends JpaRepository<PeriodoAcademico, String> {

    boolean existsByCodigo(String codigo);
}
