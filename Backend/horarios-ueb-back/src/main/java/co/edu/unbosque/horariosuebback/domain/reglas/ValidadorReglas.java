package co.edu.unbosque.horariosuebback.domain.reglas;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validador de restricciones duras (CU-11, RF-09): la misma validación al generar, al mover y al publicar. */
@Component
public class ValidadorReglas {

    private final List<ReglaDura> reglas;

    public ValidadorReglas() {
        this(ReglasDuras.TODAS);
    }

    public ValidadorReglas(List<ReglaDura> reglas) {
        this.reglas = reglas;
    }

    /** Evalúa una asignación contra todas las reglas y devuelve todas las violaciones. */
    public List<Violacion> validar(Asignacion a, List<Sesion> sesionesVersion, Parametros p, Catalogo cat) {
        Grupo grupo = cat.grupo(a.grupoId());
        if (grupo == null) return List.of(new Violacion("—", "El grupo no existe."));
        Curso curso = cat.curso(grupo.getCursoId());
        List<Sesion> otras = a.sesionId() == null
                ? sesionesVersion
                : sesionesVersion.stream().filter(s -> !s.getId().equals(a.sesionId())).toList();
        ContextoValidacion ctx = new ContextoValidacion(otras, grupo, curso, cat.docente(a.docenteId()), cat.aula(a.aulaId()),
                a.dia(), a.inicio(), a.inicio() + p.duracionSesion(), p, cat, a.sesionId() == null);
        List<Violacion> out = new ArrayList<>();
        for (ReglaDura r : reglas) {
            Violacion v = r.evaluar(ctx);
            if (v != null) out.add(v);
        }
        return out;
    }

    public record ViolacionesSesion(Sesion sesion, List<Violacion> violaciones) {
    }

    /** Revisión completa de una versión (antes de publicar, RF-12). */
    public List<ViolacionesSesion> validarVersion(List<Sesion> sesiones, Parametros p, Catalogo cat) {
        List<ViolacionesSesion> out = new ArrayList<>();
        Map<String, List<Sesion>> porGrupo = new LinkedHashMap<>();
        for (Sesion s : sesiones) {
            porGrupo.computeIfAbsent(s.getGrupoId(), k -> new ArrayList<>()).add(s);
            List<Violacion> v = validar(new Asignacion(s.getId(), s.getGrupoId(), s.getDia(), s.getInicio(), s.getDocenteId(), s.getAulaId()),
                    sesiones, p, cat);
            if (!v.isEmpty()) out.add(new ViolacionesSesion(s, v));
        }
        porGrupo.forEach((grupoId, lista) -> {
            if (lista.size() > p.sesionesMaximas()) {
                out.add(new ViolacionesSesion(lista.get(0), List.of(new Violacion("R8",
                        "El grupo tiene " + lista.size() + " sesiones y el máximo es " + p.sesionesMaximas() + "."))));
            }
        });
        return out;
    }

    public List<ReglaDura> reglas() {
        return reglas;
    }
}
