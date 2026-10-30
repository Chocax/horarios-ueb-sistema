package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.dto.Dtos.GrupoDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.OfertaCurso;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.CursoRepository;
import co.edu.unbosque.horariosuebback.repository.DocenteRepository;
import co.edu.unbosque.horariosuebback.repository.GrupoRepository;
import co.edu.unbosque.horariosuebback.repository.OfertaRepository;
import co.edu.unbosque.horariosuebback.repository.PendienteRepository;
import co.edu.unbosque.horariosuebback.repository.SesionRepository;
import co.edu.unbosque.horariosuebback.repository.VersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Grupos, demanda, inscritos y cierre (CU-08, CU-09, CU-17 · RF-07, RF-18, RF-21). */
@Service
public class GrupoService {

    private final GrupoRepository grupos;
    private final CursoRepository cursos;
    private final DocenteRepository docentes;
    private final OfertaRepository ofertas;
    private final VersionRepository versiones;
    private final SesionRepository sesiones;
    private final PendienteRepository pendientes;
    private final CargadorPeriodo cargador;
    private final BitacoraService bitacora;

    public GrupoService(GrupoRepository grupos, CursoRepository cursos, DocenteRepository docentes, OfertaRepository ofertas,
                        VersionRepository versiones, SesionRepository sesiones, PendienteRepository pendientes, CargadorPeriodo cargador,
                        BitacoraService bitacora) {
        this.grupos = grupos;
        this.cursos = cursos;
        this.docentes = docentes;
        this.ofertas = ofertas;
        this.versiones = versiones;
        this.sesiones = sesiones;
        this.pendientes = pendientes;
        this.cargador = cargador;
        this.bitacora = bitacora;
    }

    /** Ajusta el número de grupos abiertos de un curso, validando el cupo efectivo (RF-18). */
    @Transactional
    public String aplicarNumeroGrupos(String periodoId, String cursoId, int n) {
        PeriodoAcademico per = cargador.periodo(periodoId);
        Parametros p = Parametros.de(per);
        Curso curso = cursos.findById(cursoId).orElseThrow(() -> NegocioException.noEncontrado("El curso"));
        int demanda = ofertas.findById(new OfertaCurso.Llave(periodoId, cursoId)).map(OfertaCurso::getDemandaEstimada).orElse(0);
        int tope = p.cupoEfectivo();
        if (n < 1) throw new NegocioException("Debe abrirse al menos un grupo.");
        if ((double) demanda / n > tope) {
            throw new NegocioException(String.format("Con %d grupo(s) cada uno tendría %.1f estudiantes y el cupo efectivo es %d (R9).", n, (double) demanda / n, tope));
        }
        List<Grupo> actuales = grupos.findByPeriodoIdAndCursoId(periodoId, cursoId).stream()
                .filter(g -> g.getEstado() == EstadoGrupo.ABIERTO)
                .sorted(Comparator.comparing(Grupo::getCodigo))
                .toList();
        int cupo = Math.max(1, Math.min(tope, (int) Math.ceil((double) demanda / n)));
        if (demanda == 0) cupo = p.cupoMaximo();
        List<String> codigosUsados = grupos.findByPeriodoIdAndCursoId(periodoId, cursoId).stream().map(Grupo::getCodigo).toList();
        int siguiente = 1;
        for (int i = actuales.size(); i < n; i++) {
            while (codigosUsados.contains("G" + siguiente)) siguiente++;
            Grupo g = new Grupo();
            g.setId(Ids.nuevo("grp"));
            g.setPeriodoId(periodoId);
            g.setCursoId(cursoId);
            g.setCodigo("G" + siguiente++);
            // norma del Programa: los dos primeros grupos son diurnos; los siguientes, nocturnos
            g.setJornada(i < 2 ? Jornada.DIURNA : Jornada.NOCTURNA);
            g.setCupoPlaneado(cupo);
            g.setEstado(EstadoGrupo.ABIERTO);
            grupos.save(g);
        }
        for (Grupo g : actuales.subList(Math.min(n, actuales.size()), actuales.size())) quitar(g);
        for (Grupo g : actuales.subList(0, Math.min(n, actuales.size()))) g.setCupoPlaneado(cupo);
        bitacora.registrar("Ajuste de número de grupos", curso.getNombre() + ": " + n + " grupo(s) de " + cupo);
        return curso.getNombre() + ": " + n + " grupo(s) con cupo " + cupo + ".";
    }

    /** Quita un grupo sobrante: libera sus sesiones del borrador; si ya figuró en un horario publicado, solo se cierra. */
    private void quitar(Grupo g) {
        for (VersionHorario v : versiones.findByPeriodoIdAndEstado(g.getPeriodoId(), EstadoVersion.BORRADOR)) sesiones.borrarDeGrupo(v.getId(), g.getId());
        pendientes.borrarDeGrupo(g.getId());
        if (sesiones.existsByGrupoId(g.getId())) g.setEstado(EstadoGrupo.CERRADO);
        else grupos.delete(g);
    }

    @Transactional
    public GrupoDto guardarGrupo(GrupoDto d) {
        PeriodoAcademico per = cargador.periodo(d.periodoId());
        int tope = Parametros.de(per).cupoEfectivo();
        if (!Ids.valido(d.id())) throw new NegocioException("El identificador interno no es válido.");
        if (!cursos.existsById(d.cursoId())) throw NegocioException.noEncontrado("El curso");
        if (d.cupoPlaneado() <= 0) throw new NegocioException("El cupo debe ser mayor que 0.");
        if (d.cupoPlaneado() > tope) {
            throw new NegocioException("R9 · El cupo " + d.cupoPlaneado() + " supera el máximo efectivo de " + tope + " (" + per.getCupoMaximo() + " + "
                    + per.getToleranciaCupo() + " %).");
        }
        if (d.docentePreferido() != null && !docentes.existsById(d.docentePreferido())) throw NegocioException.noEncontrado("El docente preferente");
        if (!d.codigo().trim().matches("^[A-Za-z0-9-]{1,10}$")) throw new NegocioException("El código del grupo solo admite letras, números y guiones.");
        Grupo g = grupos.findById(d.id()).orElse(null);
        boolean existe = g != null;
        if (existe && !g.getPeriodoId().equals(d.periodoId())) throw new NegocioException("El grupo pertenece a otro periodo.");
        boolean repetido = grupos.findByPeriodoIdAndCursoId(d.periodoId(), d.cursoId()).stream()
                .anyMatch(x -> !x.getId().equals(d.id()) && x.getCodigo().equalsIgnoreCase(d.codigo().trim()));
        if (repetido) throw new NegocioException("Ya existe el grupo " + d.codigo() + " para este curso en el periodo.");
        if (!existe) {
            g = new Grupo();
            g.setId(d.id());
            g.setPeriodoId(d.periodoId());
            g.setCursoId(d.cursoId());
            g.setEstado(EstadoGrupo.ABIERTO);
        }
        g.setCodigo(d.codigo().trim().toUpperCase());
        g.setJornada(d.jornada());
        g.setCupoPlaneado(d.cupoPlaneado());
        g.setDocentePreferido(d.docentePreferido());
        grupos.saveAndFlush(g);
        Curso c = cursos.findById(g.getCursoId()).orElseThrow();
        bitacora.registrar(existe ? "Modificación de grupo" : "Creación de grupo", c.getNombre() + " " + g.getCodigo() + " · cupo " + g.getCupoPlaneado());
        return Mapeo.grupo(g);
    }

    @Transactional
    public GrupoDto registrarInscritos(String grupoId, Integer n) {
        if (n != null && n < 0) throw new NegocioException("El número de inscritos no puede ser negativo.");
        Grupo g = grupos.findById(grupoId).orElseThrow(() -> NegocioException.noEncontrado("El grupo"));
        g.setInscritos(n);
        Curso c = cursos.findById(g.getCursoId()).orElseThrow();
        bitacora.registrar("Registro de inscritos", c.getNombre() + " " + g.getCodigo() + ": " + (n == null ? "—" : n));
        return Mapeo.grupo(g);
    }

    /** Cierra un grupo con pocos inscritos y libera su docente y aula en las versiones vigentes (RF-21). */
    @Transactional
    public String cerrarGrupo(String grupoId) {
        Grupo g = grupos.findById(grupoId).orElseThrow(() -> NegocioException.noEncontrado("El grupo"));
        PeriodoAcademico per = cargador.periodo(g.getPeriodoId());
        int min = per.getCupoMinimo();
        if (g.getEstado() == EstadoGrupo.CERRADO) throw new NegocioException("El grupo ya está cerrado.");
        if (g.getInscritos() == null || g.getInscritos() >= min) {
            throw new NegocioException("Solo se cierran grupos con inscritos registrados por debajo del mínimo (" + min + ").");
        }
        int liberadas = 0;
        for (VersionHorario v : versiones.findByPeriodoIdOrderByNumeroDesc(g.getPeriodoId())) {
            if (v.getEstado() == EstadoVersion.ARCHIVADA) continue;
            List<Sesion> delGrupo = sesiones.findByVersionId(v.getId()).stream().filter(s -> s.getGrupoId().equals(grupoId)).toList();
            liberadas += delGrupo.size();
            sesiones.deleteAll(delGrupo);
            pendientes.findByVersionId(v.getId()).stream().filter(x -> x.getGrupoId().equals(grupoId)).forEach(pendientes::delete);
        }
        g.setEstado(EstadoGrupo.CERRADO);
        Curso c = cursos.findById(g.getCursoId()).orElseThrow();
        String etiqueta = c.getNombre() + " " + g.getCodigo();
        bitacora.registrar("Cierre de grupo", etiqueta + " con " + g.getInscritos() + " inscritos; " + liberadas + " sesión(es) liberadas (docente y aula)");
        return etiqueta + " cerrado. Se liberaron " + liberadas + " sesión(es).";
    }
}
