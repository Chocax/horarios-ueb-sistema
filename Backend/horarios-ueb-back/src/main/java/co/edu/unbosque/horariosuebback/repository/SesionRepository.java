package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SesionRepository extends JpaRepository<Sesion, String> {

    List<Sesion> findByVersionId(String versionId);

    List<Sesion> findByVersionIdIn(Collection<String> versionIds);

    boolean existsByGrupoId(String grupoId);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from Sesion s where s.versionId = ?1 and s.grupoId = ?2")
    int borrarDeGrupo(String versionId, String grupoId);
}
