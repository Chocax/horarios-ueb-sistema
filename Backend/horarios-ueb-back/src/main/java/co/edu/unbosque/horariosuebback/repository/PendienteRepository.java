package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.GrupoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PendienteRepository extends JpaRepository<GrupoPendiente, GrupoPendiente.Llave> {

    List<GrupoPendiente> findByVersionId(String versionId);

    List<GrupoPendiente> findByVersionIdIn(Collection<String> versionIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from GrupoPendiente p where p.grupoId = ?1")
    int borrarDeGrupo(String grupoId);
}
