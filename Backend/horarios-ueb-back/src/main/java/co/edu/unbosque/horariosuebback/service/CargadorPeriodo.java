package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.AulaRepository;
import co.edu.unbosque.horariosuebback.repository.CursoRepository;
import co.edu.unbosque.horariosuebback.repository.DocenteRepository;
import co.edu.unbosque.horariosuebback.repository.GrupoRepository;
import co.edu.unbosque.horariosuebback.repository.PeriodoRepository;
import co.edu.unbosque.horariosuebback.repository.SesionRepository;
import co.edu.unbosque.horariosuebback.repository.VersionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Carga lo que necesitan las reglas y el generador para trabajar sobre un periodo. */
@Component
public class CargadorPeriodo {

    public record Datos(PeriodoAcademico periodo, Parametros parametros, Catalogo catalogo, List<Grupo> grupos) {
    }

    private final PeriodoRepository periodos;
    private final CursoRepository cursos;
    private final DocenteRepository docentes;
    private final AulaRepository aulas;
    private final GrupoRepository grupos;
    private final VersionRepository versiones;
    private final SesionRepository sesiones;

    public CargadorPeriodo(PeriodoRepository periodos, CursoRepository cursos, DocenteRepository docentes, AulaRepository aulas,
                           GrupoRepository grupos, VersionRepository versiones, SesionRepository sesiones) {
        this.periodos = periodos;
        this.cursos = cursos;
        this.docentes = docentes;
        this.aulas = aulas;
        this.grupos = grupos;
        this.versiones = versiones;
        this.sesiones = sesiones;
    }

    public PeriodoAcademico periodo(String id) {
        return periodos.findById(id == null ? "" : id).orElseThrow(() -> NegocioException.noEncontrado("El periodo"));
    }

    public Datos cargar(String periodoId) {
        PeriodoAcademico p = periodo(periodoId);
        List<Grupo> gs = grupos.findByPeriodoId(p.getId());
        Catalogo cat = new Catalogo(cursos.findAll(), gs, docentes.findAll(), aulas.findAll());
        return new Datos(p, Parametros.de(p), cat, gs);
    }

    /** Versión vigente del periodo: el borrador si existe; si no, la publicada. */
    public Optional<VersionHorario> vigente(String periodoId) {
        return versiones.findFirstByPeriodoIdAndEstado(periodoId, EstadoVersion.BORRADOR)
                .or(() -> versiones.findFirstByPeriodoIdAndEstado(periodoId, EstadoVersion.PUBLICADA));
    }

    public List<Sesion> sesionesVigentes(String periodoId) {
        return vigente(periodoId).map(v -> sesiones.findByVersionId(v.getId())).orElse(List.of());
    }
}
