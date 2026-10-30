package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, String> {

    List<Grupo> findByPeriodoId(String periodoId);

    List<Grupo> findByPeriodoIdAndEstado(String periodoId, EstadoGrupo estado);

    List<Grupo> findByPeriodoIdAndCursoId(String periodoId, String cursoId);
}
