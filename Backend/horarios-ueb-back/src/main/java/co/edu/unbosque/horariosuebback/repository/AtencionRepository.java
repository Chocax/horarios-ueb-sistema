package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionRepository extends JpaRepository<HorarioAtencion, String> {

    List<HorarioAtencion> findByPeriodoId(String periodoId);

    List<HorarioAtencion> findByPeriodoIdAndDocenteId(String periodoId, String docenteId);

    Optional<HorarioAtencion> findByPeriodoIdAndDocenteIdAndCursoId(String periodoId, String docenteId, String cursoId);
}
