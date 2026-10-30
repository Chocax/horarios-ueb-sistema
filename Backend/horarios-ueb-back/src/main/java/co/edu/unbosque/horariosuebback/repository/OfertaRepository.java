package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.OfertaCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<OfertaCurso, OfertaCurso.Llave> {

    List<OfertaCurso> findByPeriodoId(String periodoId);
}
