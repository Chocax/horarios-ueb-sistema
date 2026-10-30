package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<VersionHorario, String> {

    List<VersionHorario> findByPeriodoIdOrderByNumeroDesc(String periodoId);

    List<VersionHorario> findByPeriodoIdAndEstado(String periodoId, EstadoVersion estado);

    Optional<VersionHorario> findFirstByPeriodoIdAndEstado(String periodoId, EstadoVersion estado);

    @Query("select coalesce(max(v.numero), 0) from VersionHorario v where v.periodoId = ?1")
    int maxNumero(String periodoId);
}
