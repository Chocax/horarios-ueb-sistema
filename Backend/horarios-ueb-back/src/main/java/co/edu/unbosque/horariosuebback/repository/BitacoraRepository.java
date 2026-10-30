package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.RegistroBitacora;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BitacoraRepository extends JpaRepository<RegistroBitacora, Long> {

    List<RegistroBitacora> findAllByOrderByIdDesc(Pageable pagina);
}
