package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.dto.Dtos.AtencionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.DocenteDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.EstadoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ObservacionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.OfertaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.PeriodoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.VersionDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.GrupoPendiente;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoPeriodo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import co.edu.unbosque.horariosuebback.repository.AtencionRepository;
import co.edu.unbosque.horariosuebback.repository.AulaRepository;
import co.edu.unbosque.horariosuebback.repository.CursoRepository;
import co.edu.unbosque.horariosuebback.repository.DocenteRepository;
import co.edu.unbosque.horariosuebback.repository.GrupoRepository;
import co.edu.unbosque.horariosuebback.repository.ObservacionRepository;
import co.edu.unbosque.horariosuebback.repository.OfertaRepository;
import co.edu.unbosque.horariosuebback.repository.PendienteRepository;
import co.edu.unbosque.horariosuebback.repository.PeriodoRepository;
import co.edu.unbosque.horariosuebback.repository.SesionRepository;
import co.edu.unbosque.horariosuebback.repository.VersionRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Estado que el front carga al iniciar sesión o al cambiar de periodo. Se filtra según el rol (RF-19, RNF-08):
 * el docente solo recibe su propio horario publicado y sus datos; los usuarios solo los recibe el administrador.
 * Las sesiones se envían únicamente para las versiones del periodo seleccionado.
 */
@Service
public class EstadoService {

    private final PeriodoRepository periodos;
    private final CursoRepository cursos;
    private final DocenteRepository docentes;
    private final AulaRepository aulas;
    private final OfertaRepository ofertas;
    private final GrupoRepository grupos;
    private final VersionRepository versiones;
    private final SesionRepository sesiones;
    private final PendienteRepository pendientes;
    private final ObservacionRepository observaciones;
    private final AtencionRepository atenciones;
    private final UsuarioService usuarios;
    private final SesionActual sesion;

    public EstadoService(PeriodoRepository periodos, CursoRepository cursos, DocenteRepository docentes, AulaRepository aulas,
                         OfertaRepository ofertas, GrupoRepository grupos, VersionRepository versiones, SesionRepository sesiones,
                         PendienteRepository pendientes, ObservacionRepository observaciones, AtencionRepository atenciones,
                         UsuarioService usuarios, SesionActual sesion) {
        this.periodos = periodos;
        this.cursos = cursos;
        this.docentes = docentes;
        this.aulas = aulas;
        this.ofertas = ofertas;
        this.grupos = grupos;
        this.versiones = versiones;
        this.sesiones = sesiones;
        this.pendientes = pendientes;
        this.observaciones = observaciones;
        this.atenciones = atenciones;
        this.usuarios = usuarios;
        this.sesion = sesion;
    }

    @Transactional(readOnly = true)
    public EstadoDto estado(String periodoPedido) {
        UsuarioActual u = sesion.exigir();
        boolean docente = u.rol() == Rol.DOCENTE;
        List<PeriodoAcademico> pers = periodos.findAll().stream().sorted(Comparator.comparing(PeriodoAcademico::getCodigo)).toList();
        String activo = pers.stream().map(PeriodoAcademico::getId).filter(id -> id.equals(periodoPedido)).findFirst()
                .orElseGet(() -> periodoPorDefecto(pers));

        List<VersionHorario> vs = versiones.findAll().stream()
                .filter(v -> !docente || v.getEstado() == EstadoVersion.PUBLICADA)
                .sorted(Comparator.comparing(VersionHorario::getPeriodoId).thenComparing(VersionHorario::getNumero))
                .toList();
        List<String> idsActivo = vs.stream().filter(v -> v.getPeriodoId().equals(activo)).map(VersionHorario::getId).toList();
        Map<String, List<Sesion>> ses = sesiones.findByVersionIdIn(idsActivo).stream()
                .filter(s -> !docente || Objects.equals(s.getDocenteId(), u.docenteId()))
                .collect(Collectors.groupingBy(Sesion::getVersionId));
        Map<String, List<GrupoPendiente>> pend = docente ? Map.of()
                : pendientes.findByVersionIdIn(idsActivo).stream().collect(Collectors.groupingBy(GrupoPendiente::getVersionId));
        List<VersionDto> versionesDto = vs.stream()
                .map(v -> Mapeo.version(v, ses.getOrDefault(v.getId(), List.of()), pend.getOrDefault(v.getId(), List.of())))
                .toList();

        List<DocenteDto> docentesDto = docentes.findAll().stream()
                .filter(d -> !docente || d.getId().equals(u.docenteId()))
                .map(Mapeo::docente).toList();
        List<ObservacionDto> obs = docente ? List.of()
                : observaciones.findByVersionIdInOrderByFechaAsc(idsActivo).stream().map(Mapeo::observacion).toList();
        List<AtencionDto> ate = atenciones.findByPeriodoId(activo).stream()
                .filter(h -> !docente || h.getDocenteId().equals(u.docenteId()))
                .map(Mapeo::atencion).toList();
        List<UsuarioDto> us = u.rol() == Rol.ADMINISTRADOR ? usuarios.listar() : List.of();
        List<PeriodoDto> persDto = pers.stream().map(Mapeo::periodo).toList();
        List<OfertaDto> ofs = ofertas.findByPeriodoId(activo).stream().map(Mapeo::oferta).toList();

        return new EstadoDto(persDto, cursos.findAll().stream().map(Mapeo::curso).toList(), ofs, docentesDto,
                aulas.findAll().stream().map(Mapeo::aula).toList(), grupos.findAll().stream().map(Mapeo::grupo).toList(),
                versionesDto, obs, us, ate, activo);
    }

    /** El periodo abierto más reciente; si no hay, el más reciente. */
    private static String periodoPorDefecto(List<PeriodoAcademico> pers) {
        return pers.stream().filter(p -> p.getEstado() == EstadoPeriodo.ABIERTO).map(PeriodoAcademico::getId)
                .reduce((a, b) -> b)
                .orElse(pers.isEmpty() ? "" : pers.get(pers.size() - 1).getId());
    }
}
