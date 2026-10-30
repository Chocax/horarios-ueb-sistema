package co.edu.unbosque.horariosuebback.domain.reglas;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.domain.Textos;
import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;

import java.util.List;

/**
 * Lo que necesita una regla para evaluarse.
 *
 * @param otras  sesiones de la versión SIN la sesión que se evalúa
 * @param esNueva true si la sesión se agrega al grupo (no es un movimiento de una existente)
 */
public record ContextoValidacion(
        List<Sesion> otras,
        Grupo grupo,
        Curso curso,
        Docente docente,
        Aula aula,
        int dia,
        int inicio,
        int fin,
        Parametros parametros,
        Catalogo catalogo,
        boolean esNueva) {

    /** «Cálculo diferencial G1 · lunes 07:00–09:00» */
    public String etiqueta(Sesion s) {
        Grupo g = catalogo.grupo(s.getGrupoId());
        Curso c = g == null ? null : catalogo.curso(g.getCursoId());
        int dur = parametros.duracionSesion();
        return (c == null ? "otra clase" : c.getNombre()) + " " + (g == null ? "" : g.getCodigo()) + " · "
                + Textos.dia(s.getDia()) + " " + Textos.rango(s.getInicio(), s.getInicio() + dur);
    }

    public boolean solapa(Sesion s) {
        int dur = parametros.duracionSesion();
        return s.getDia() == dia && s.getInicio() < fin && inicio < s.getInicio() + dur;
    }
}
