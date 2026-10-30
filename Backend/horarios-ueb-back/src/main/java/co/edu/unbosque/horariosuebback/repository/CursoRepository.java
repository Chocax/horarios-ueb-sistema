package co.edu.unbosque.horariosuebback.repository;

import co.edu.unbosque.horariosuebback.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, String> {

    Optional<Curso> findByCodigo(String codigo);
}
