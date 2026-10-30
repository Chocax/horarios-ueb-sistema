package co.edu.unbosque.horariosuebback.domain.generador;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;

import java.util.Collection;
import java.util.List;

/** Estrategia de generación de la propuesta (CU-10, RF-08). Se consume por esta interfaz (RNF-04). */
public interface GeneradorHorario {

    String nombre();

    Resultado generar(Entrada entrada);

    /** Horas del docente que no se usan para clase (p. ej. su horario de atención). */
    record Bloqueo(String docenteId, int dia, int inicio, int fin) {
    }

    /**
     * @param sesionesFijas sesiones ya ubicadas que se respetan (reintento de un pendiente)
     */
    record Entrada(List<Grupo> grupos, Collection<Docente> docentes, Collection<Aula> aulas, Parametros parametros, Catalogo catalogo,
                   List<Sesion> sesionesFijas, List<Bloqueo> bloqueosDocente) {
    }

    record Pendiente(String grupoId, String regla, String causa) {
    }

    /** Las sesiones vienen sin versionId: el servicio las asocia a la versión al guardarlas. */
    record Resultado(List<Sesion> sesiones, List<Pendiente> pendientes, long milisegundos) {
    }
}
