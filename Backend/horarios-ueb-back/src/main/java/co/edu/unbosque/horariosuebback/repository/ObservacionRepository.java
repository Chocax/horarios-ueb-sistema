package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.Observacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ObservacionRepository extends JpaRepository<Observacion, String> {

    List<Observacion> findByVersionIdInOrderByFechaAsc(Collection<String> versionIds);
}
