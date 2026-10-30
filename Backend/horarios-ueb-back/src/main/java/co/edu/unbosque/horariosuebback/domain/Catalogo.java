package co.edu.unbosque.horariosuebback.domain;

import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Búsqueda por id de cursos, grupos, docentes y aulas; lo usan las reglas y el generador. */
public class Catalogo {

    private final Map<String, Curso> cursos;
    private final Map<String, Grupo> grupos;
    private final Map<String, Docente> docentes;
    private final Map<String, Aula> aulas;

    public Catalogo(Collection<Curso> cursos, Collection<Grupo> grupos, Collection<Docente> docentes, Collection<Aula> aulas) {
        this.cursos = cursos.stream().collect(Collectors.toMap(Curso::getId, Function.identity()));
        this.grupos = grupos.stream().collect(Collectors.toMap(Grupo::getId, Function.identity()));
        this.docentes = docentes.stream().collect(Collectors.toMap(Docente::getId, Function.identity()));
        this.aulas = aulas.stream().collect(Collectors.toMap(Aula::getId, Function.identity()));
    }

    public Curso curso(String id) {
        return id == null ? null : cursos.get(id);
    }

    public Grupo grupo(String id) {
        return id == null ? null : grupos.get(id);
    }

    public Docente docente(String id) {
        return id == null ? null : docentes.get(id);
    }

    public Aula aula(String id) {
        return id == null ? null : aulas.get(id);
    }

    public Curso cursoDeGrupo(String grupoId) {
        Grupo g = grupo(grupoId);
        return g == null ? null : curso(g.getCursoId());
    }

    public Collection<Docente> docentes() {
        return docentes.values();
    }

    public Collection<Aula> aulas() {
        return aulas.values();
    }

    /** «Programación I G2» */
    public String etiquetaGrupo(String grupoId) {
        Grupo g = grupo(grupoId);
        Curso c = g == null ? null : curso(g.getCursoId());
        return g != null && c != null ? c.getNombre() + " " + g.getCodigo() : "—";
    }
}
