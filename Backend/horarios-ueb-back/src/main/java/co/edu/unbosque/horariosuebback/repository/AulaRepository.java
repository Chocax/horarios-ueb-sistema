package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, String> {

    Optional<Aula> findByCodigo(String codigo);
}
