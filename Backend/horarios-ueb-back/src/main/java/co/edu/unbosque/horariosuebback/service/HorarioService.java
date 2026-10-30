package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.domain.Textos;
import co.edu.unbosque.horariosuebback.domain.generador.GeneradorHorario;
import co.edu.unbosque.horariosuebback.domain.reglas.Asignacion;
import co.edu.unbosque.horariosuebback.domain.reglas.ValidadorReglas;
import co.edu.unbosque.horariosuebback.domain.reglas.Violacion;
import co.edu.unbosque.horariosuebback.dto.Dtos.MoverSesionRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.ObservacionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.dto.Dtos.SesionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.VersionDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.GrupoPendiente;
import co.edu.unbosque.horariosuebback.entity.Observacion;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.Concepto;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoPeriodo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import co.edu.unbosque.horariosuebback.entity.enums.ModoAtencion;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.AtencionRepository;
import co.edu.unbosque.horariosuebback.repository.ObservacionRepository;
import co.edu.unbosque.horariosuebback.repository.PendienteRepository;
import co.edu.unbosque.horariosuebback.repository.SesionRepository;
import co.edu.unbosque.horariosuebback.repository.VersionRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Generación, ajuste, publicación y restauración de versiones del horario (CU-10 a CU-15, CU-20). */
@Service
public class HorarioService {

    private final CargadorPeriodo cargador;
    private final GeneradorHorario generador;
    private final ValidadorReglas validador;
    private final VersionRepository versiones;
    private final SesionRepository sesiones;
    private final PendienteRepository pendientes;
    private final ObservacionRepository observaciones;
    private final AtencionRepository atenciones;
    private final BitacoraService bitacora;
    private final SesionActual sesion;
    private final EntityManager em;

    public HorarioService(CargadorPeriodo cargador, GeneradorHorario generador, ValidadorReglas validador, VersionRepository versiones,
                          SesionRepository sesiones, PendienteRepository pendientes, ObservacionRepository observaciones,
                          AtencionRepository atenciones, BitacoraService bitacora, SesionActual sesion, EntityManager em) {
        this.cargador = cargador;
        this.generador = generador;
        this.validador = validador;
        this.versiones = versiones;
        this.sesiones = sesiones;
        this.pendientes = pendientes;
        this.observaciones = observaciones;
        this.atenciones = atenciones;
        this.bitacora = bitacora;
        this.sesion = sesion;
        this.em = em;
    }

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public VersionDto version(String id) {
        return dto(buscar(id));
    }

    VersionDto dto(VersionHorario v) {
        return Mapeo.version(v, sesiones.findByVersionId(v.getId()), pendientes.findByVersionId(v.getId()));
    }

    // ------------------------------------------------------------------ generación (CU-10)

    @Transactional
    public VersionDto generar(String periodoId) {
        CargadorPeriodo.Datos d = cargador.cargar(periodoId);
        PeriodoAcademico per = d.periodo();
        if (per.getEstado() == EstadoPeriodo.CERRADO) throw new NegocioException("El periodo " + per.getCodigo() + " está cerrado.");
        List<Grupo> abiertos = d.grupos().stream().filter(g -> g.getEstado() == EstadoGrupo.ABIERTO).toList();
        GeneradorHorario.Resultado r = generador.generar(new GeneradorHorario.Entrada(abiertos, d.catalogo().docentes(), d.catalogo().aulas(),
                d.parametros(), d.catalogo(), List.of(), bloqueosAtencion(per.getId())));
        archivarBorradores(per.getId());
        VersionHorario v = nuevaVersion(per.getId(), "Generada automáticamente (" + generador.nombre() + ", " + r.milisegundos() + " ms)");
        guardarSesiones(v, r.sesiones());
        pendientes.saveAll(r.pendientes().stream().map(p -> new GrupoPendiente(v.getId(), p.grupoId(), p.regla(), recortar(p.causa()))).toList());
        bitacora.registrar("Generación de propuesta", "Versión " + v.getNumero() + " de " + per.getCodigo() + ": " + r.sesiones().size()
                + " sesiones, " + r.pendientes().size() + " grupo(s) pendiente(s), " + r.milisegundos() + " ms");
        return dto(v);
    }

    /** Bloques de atención con hora fija: el generador no programa clases del docente encima (RF-23). */
    private List<GeneradorHorario.Bloqueo> bloqueosAtencion(String periodoId) {
        return atenciones.findByPeriodoId(periodoId).stream()
                .filter(h -> h.getModo() == ModoAtencion.HORARIO && h.getDia() != null && h.getInicio() != null && h.getFin() != null)
                .map(h -> new GeneradorHorario.Bloqueo(h.getDocenteId(), h.getDia(), h.getInicio(), h.getFin()))
                .toList();
    }

    // ------------------------------------------------------------------ ajuste en el tablero (CU-11, CU-12)

    /** Mueve o reasigna una sesión: valida R1–R9 y, si pasa, guarda el cambio (RF-10). Devuelve solo la sesión (respuesta liviana). */
    @Transactional
    public SesionDto mover(String versionId, String sesionId, MoverSesionRequest req) {
        VersionHorario v = borrador(versionId);
        Sesion s = sesiones.findById(sesionId).filter(x -> x.getVersionId().equals(v.getId()))
                .orElseThrow(() -> NegocioException.noEncontrado("La sesión"));
        CargadorPeriodo.Datos d = cargador.cargar(v.getPeriodoId());
        List<Sesion> todas = sesiones.findByVersionId(v.getId());
        List<Violacion> viol = validador.validar(new Asignacion(s.getId(), s.getGrupoId(), req.dia(), req.inicio(), req.docenteId(), req.aulaId()),
                todas, d.parametros(), d.catalogo());
        if (!viol.isEmpty()) throw NegocioException.violaciones("Movimiento rechazado", viol);

        Catalogo cat = d.catalogo();
        int dur = d.parametros().duracionSesion();
        StringBuilder det = new StringBuilder(cat.etiquetaGrupo(s.getGrupoId())).append(": ")
                .append(Textos.dia(s.getDia())).append(' ').append(Textos.rango(s.getInicio(), s.getInicio() + dur))
                .append(" → ").append(Textos.dia(req.dia())).append(' ').append(Textos.rango(req.inicio(), req.inicio() + dur));
        if (!Objects.equals(req.aulaId(), s.getAulaId())) det.append(", aula ").append(cat.aula(req.aulaId()).getCodigo());
        if (!Objects.equals(req.docenteId(), s.getDocenteId())) det.append(", docente ").append(cat.docente(req.docenteId()).getNombre());
        s.setDia(req.dia());
        s.setInicio(req.inicio());
        s.setDocenteId(req.docenteId());
        s.setAulaId(req.aulaId());
        sesiones.save(s);
        bitacora.registrar("Ajuste de sesión", det.append(" (v").append(v.getNumero()).append(')').toString());
        return Mapeo.sesion(s);
    }

    /** Ubica una sesión de un grupo pendiente en el día y hora indicados, eligiendo docente y aula válidos. */
    @Transactional
    public Respuesta<VersionDto> ubicarPendiente(String versionId, String grupoId, int dia, int inicio) {
        VersionHorario v = borrador(versionId);
        CargadorPeriodo.Datos d = cargador.cargar(v.getPeriodoId());
        Catalogo cat = d.catalogo();
        Grupo g = grupoAbierto(cat, grupoId);
        Parametros p = d.parametros();
        List<Sesion> todas = sesiones.findByVersionId(v.getId());
        List<Sesion> previas = todas.stream().filter(s -> s.getGrupoId().equals(grupoId)).toList();
        // norma del Programa (blanda, B6): en el día se prefieren docentes distintos para los grupos del mismo curso
        Set<String> otrosDiurnos = g.getJornada() != Jornada.DIURNA ? Set.of() : todas.stream()
                .filter(s -> !s.getGrupoId().equals(grupoId))
                .filter(s -> {
                    Grupo og = cat.grupo(s.getGrupoId());
                    return og != null && og.getCursoId().equals(g.getCursoId()) && og.getJornada() == Jornada.DIURNA;
                })
                .map(Sesion::getDocenteId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Docente> candidatos = !previas.isEmpty() && previas.get(0).getDocenteId() != null
                ? List.of(cat.docente(previas.get(0).getDocenteId()))
                : cat.docentes().stream().filter(x -> x.getHabilitaciones().contains(g.getCursoId()))
                .sorted(Comparator.comparing((Docente x) -> otrosDiurnos.contains(x.getId())).thenComparing(Docente::getId))
                .toList();
        if (candidatos.isEmpty()) {
            throw NegocioException.violaciones("No hay docente y aula válidos para esa franja",
                    List.of(new Violacion("R5", "Ningún docente está habilitado para " + cat.curso(g.getCursoId()).getNombre() + ".")));
        }
        List<co.edu.unbosque.horariosuebback.entity.Aula> aulas = cat.aulas().stream()
                .sorted(Comparator.comparingInt(co.edu.unbosque.horariosuebback.entity.Aula::getCapacidad)
                        .thenComparing(co.edu.unbosque.horariosuebback.entity.Aula::getCodigo))
                .toList();
        List<Violacion> mejor = null;
        for (Docente doc : candidatos) {
            for (var a : aulas) {
                List<Violacion> viol = validador.validar(new Asignacion(null, grupoId, dia, inicio, doc.getId(), a.getId()), todas, p, cat);
                if (viol.isEmpty()) {
                    Sesion s = new Sesion(Ids.nuevo("ses"), v.getId(), grupoId, dia, inicio, doc.getId(), a.getId());
                    sesiones.save(s);
                    int total = previas.size() + 1;
                    int requeridas = Math.max(p.sesionesMinimas(), cat.curso(g.getCursoId()).getSesionesSemanales());
                    if (total >= requeridas) pendientes.deleteById(new GrupoPendiente.Llave(v.getId(), grupoId));
                    bitacora.registrar("Ubicación manual de sesión", cat.etiquetaGrupo(grupoId) + " (" + total + "/" + requeridas + "): "
                            + Textos.dia(dia) + " " + Textos.rango(inicio, inicio + p.duracionSesion()) + ", " + a.getCodigo() + ", " + doc.getNombre()
                            + " (v" + v.getNumero() + ")");
                    pendientes.flush();
                    return Respuesta.ok("Sesión " + total + " de " + requeridas + " ubicada en " + a.getCodigo() + " con " + doc.getNombre() + ".", dto(v));
                }
                if (mejor == null || viol.size() < mejor.size()) mejor = viol;
            }
        }
        throw NegocioException.violaciones("No hay docente y aula válidos para esa franja", mejor == null ? List.of() : mejor);
    }

    /** Reintenta ubicar automáticamente un grupo pendiente (por ejemplo, tras corregir su causa). */
    @Transactional
    public Respuesta<VersionDto> reintentarPendiente(String versionId, String grupoId) {
        VersionHorario v = borrador(versionId);
        CargadorPeriodo.Datos d = cargador.cargar(v.getPeriodoId());
        Catalogo cat = d.catalogo();
        Grupo g = grupoAbierto(cat, grupoId);
        List<Sesion> otras = sesiones.findByVersionId(v.getId()).stream().filter(s -> !s.getGrupoId().equals(grupoId)).toList();
        GeneradorHorario.Resultado r = generador.generar(new GeneradorHorario.Entrada(List.of(g), cat.docentes(), cat.aulas(), d.parametros(), cat,
                otras, bloqueosAtencion(v.getPeriodoId())));
        if (!r.pendientes().isEmpty() || r.sesiones().isEmpty()) {
            GeneradorHorario.Pendiente causa = r.pendientes().isEmpty()
                    ? new GeneradorHorario.Pendiente(grupoId, "R1", "Las franjas libres chocan con otras sesiones de la versión.")
                    : r.pendientes().get(0);
            pendientes.save(new GrupoPendiente(v.getId(), grupoId, causa.regla(), recortar(causa.causa())));
            throw new NegocioException("El grupo sigue pendiente", List.of(causa.regla() + " · " + causa.causa()));
        }
        sesiones.borrarDeGrupo(v.getId(), grupoId);
        guardarSesiones(v, r.sesiones());
        pendientes.deleteById(new GrupoPendiente.Llave(v.getId(), grupoId));
        bitacora.registrar("Ubicación automática de pendiente", cat.etiquetaGrupo(grupoId) + ": " + r.sesiones().size() + " sesión(es) (v" + v.getNumero() + ")");
        pendientes.flush();
        return Respuesta.ok(cat.etiquetaGrupo(grupoId) + " quedó ubicado con " + r.sesiones().size() + " sesión(es).", dto(v));
    }

    // ------------------------------------------------------------------ versiones (CU-14, CU-15)

    @Transactional
    public VersionDto publicar(String versionId) {
        VersionHorario v = buscar(versionId);
        CargadorPeriodo.Datos d = cargador.cargar(v.getPeriodoId());
        Catalogo cat = d.catalogo();
        List<String> causas = new ArrayList<>();
        if (v.getEstado() != EstadoVersion.BORRADOR) causas.add("Solo se puede publicar la versión de trabajo (borrador).");
        List<GrupoPendiente> pend = pendientes.findByVersionId(v.getId());
        pend.forEach(x -> causas.add("Pendiente · " + cat.etiquetaGrupo(x.getGrupoId()) + ": " + x.getCausa()));
        List<Sesion> todas = sesiones.findByVersionId(v.getId());
        validador.validarVersion(todas, d.parametros(), cat).forEach(x -> x.violaciones()
                .forEach(w -> causas.add(w.regla() + " · " + cat.etiquetaGrupo(x.sesion().getGrupoId()) + ": " + w.mensaje())));
        Set<String> conPendiente = pend.stream().map(GrupoPendiente::getGrupoId).collect(Collectors.toSet());
        Map<String, Long> porGrupo = todas.stream().collect(Collectors.groupingBy(Sesion::getGrupoId, Collectors.counting()));
        d.grupos().stream().filter(g -> g.getEstado() == EstadoGrupo.ABIERTO).forEach(g -> {
            long n = porGrupo.getOrDefault(g.getId(), 0L);
            int req = Math.max(d.parametros().sesionesMinimas(), cat.curso(g.getCursoId()).getSesionesSemanales());
            if (n < req && !conPendiente.contains(g.getId())) causas.add("Incompleto · " + cat.etiquetaGrupo(g.getId()) + ": " + n + " de " + req + " sesiones.");
        });
        if (!causas.isEmpty()) {
            bitacora.registrarAparte(sesion.nombre(), sesion.rol(), "Publicación bloqueada", "Versión " + v.getNumero() + ": " + causas.size() + " causa(s)");
            throw new NegocioException("No se puede publicar la versión " + v.getNumero(), causas);
        }
        versiones.findByPeriodoIdAndEstado(v.getPeriodoId(), EstadoVersion.PUBLICADA).forEach(x -> x.setEstado(EstadoVersion.ARCHIVADA));
        versiones.flush();
        v.setEstado(EstadoVersion.PUBLICADA);
        v.setFechaPublicacion(Instant.now());
        bitacora.registrar("Publicación de horario", "Versión " + v.getNumero() + " de " + d.periodo().getCodigo() + " publicada como oficial");
        return dto(v);
    }

    @Transactional
    public VersionDto restaurar(String versionId) {
        VersionHorario origen = buscar(versionId);
        PeriodoAcademico per = cargador.periodo(origen.getPeriodoId());
        if (per.getEstado() == EstadoPeriodo.CERRADO) throw new NegocioException("El periodo " + per.getCodigo() + " está cerrado.");
        CargadorPeriodo.Datos d = cargador.cargar(per.getId());
        // los grupos cerrados después no vuelven
        Set<String> cerrados = d.grupos().stream().filter(g -> g.getEstado() == EstadoGrupo.CERRADO).map(Grupo::getId).collect(Collectors.toSet());
        List<Sesion> copia = sesiones.findByVersionId(origen.getId()).stream().filter(s -> !cerrados.contains(s.getGrupoId()))
                .map(s -> new Sesion(Ids.nuevo("ses"), null, s.getGrupoId(), s.getDia(), s.getInicio(), s.getDocenteId(), s.getAulaId()))
                .toList();
        List<GrupoPendiente> pend = pendientes.findByVersionId(origen.getId()).stream().filter(p -> !cerrados.contains(p.getGrupoId())).toList();
        archivarBorradores(per.getId());
        VersionHorario v = nuevaVersion(per.getId(), "Restaurada desde la versión " + origen.getNumero());
        guardarSesiones(v, copia);
        pendientes.saveAll(pend.stream().map(p -> new GrupoPendiente(v.getId(), p.getGrupoId(), p.getRegla(), p.getCausa())).toList());
        bitacora.registrar("Restauración de versión", "Versión " + origen.getNumero() + " → nueva versión de trabajo " + v.getNumero());
        return dto(v);
    }

    // ------------------------------------------------------------------ administración: borrar versiones de prueba

    /**
     * Borra una versión con sus sesiones, pendientes y observaciones, como si nunca hubiera existido (la bitácora conserva el
     * registro de la eliminación). La numeración del periodo sigue desde la versión más alta que quede.
     */
    @Transactional
    public void eliminarVersion(String versionId) {
        VersionHorario v = buscar(versionId);
        PeriodoAcademico per = cargador.periodo(v.getPeriodoId());
        int nSesiones = sesiones.findByVersionId(v.getId()).size();
        versiones.delete(v);
        versiones.flush();
        bitacora.registrar("Eliminación de versión", "Versión " + v.getNumero() + " de " + per.getCodigo() + " (" + v.getEstado().name().toLowerCase()
                + ", " + nSesiones + " sesiones)");
    }

    /** Borra todas las versiones del periodo; la publicada solo si se pide explícitamente. Devuelve cuántas borró. */
    @Transactional
    public int eliminarVersionesPeriodo(String periodoId, boolean incluirPublicada) {
        PeriodoAcademico per = cargador.periodo(periodoId);
        List<VersionHorario> lista = versiones.findByPeriodoIdOrderByNumeroDesc(periodoId).stream()
                .filter(v -> incluirPublicada || v.getEstado() != EstadoVersion.PUBLICADA)
                .toList();
        versiones.deleteAll(lista);
        versiones.flush();
        bitacora.registrar("Eliminación de versiones", lista.size() + " versión(es) de " + per.getCodigo()
                + (incluirPublicada ? " (incluida la publicada)" : " (se conservó la publicada)"));
        return lista.size();
    }

    // ------------------------------------------------------------------ comité (CU-20)

    @Transactional
    public ObservacionDto registrarObservacion(String versionId, Concepto concepto, String texto) {
        UsuarioActual u = sesion.exigir();
        VersionHorario v = buscar(versionId);
        if (texto == null || texto.isBlank()) throw new NegocioException("Escriba la observación.");
        Observacion o = new Observacion();
        o.setId(Ids.nuevo("obs"));
        o.setVersionId(v.getId());
        o.setUsuarioId(u.id());
        o.setAutor(u.nombre());
        o.setConcepto(concepto);
        o.setTexto(texto.trim());
        o.setFecha(Instant.now());
        observaciones.save(o);
        bitacora.registrar("Observación del comité", "Versión " + v.getNumero() + ": " + (concepto == Concepto.FAVORABLE ? "concepto favorable" : "requiere ajustes"));
        return Mapeo.observacion(o);
    }

    // ------------------------------------------------------------------ internos

    private VersionHorario buscar(String id) {
        return versiones.findById(id).orElseThrow(() -> NegocioException.noEncontrado("La versión"));
    }

    private VersionHorario borrador(String id) {
        VersionHorario v = buscar(id);
        if (v.getEstado() != EstadoVersion.BORRADOR) throw new NegocioException("Solo se puede ajustar la versión de trabajo (borrador).");
        if (cargador.periodo(v.getPeriodoId()).getEstado() == EstadoPeriodo.CERRADO) throw new NegocioException("El periodo está cerrado.");
        return v;
    }

    private static Grupo grupoAbierto(Catalogo cat, String grupoId) {
        Grupo g = cat.grupo(grupoId);
        if (g == null) throw NegocioException.noEncontrado("El grupo");
        if (g.getEstado() != EstadoGrupo.ABIERTO) throw new NegocioException("El grupo está cerrado.");
        return g;
    }

    /** Solo una versión de trabajo por periodo: los borradores anteriores se archivan. */
    private void archivarBorradores(String periodoId) {
        versiones.findByPeriodoIdAndEstado(periodoId, EstadoVersion.BORRADOR).forEach(x -> x.setEstado(EstadoVersion.ARCHIVADA));
        versiones.flush();
    }

    private VersionHorario nuevaVersion(String periodoId, String origen) {
        VersionHorario v = new VersionHorario();
        v.setId(Ids.nuevo("ver"));
        v.setPeriodoId(periodoId);
        v.setNumero(versiones.maxNumero(periodoId) + 1);
        v.setEstado(EstadoVersion.BORRADOR);
        v.setOrigen(origen);
        v.setFechaCreacion(Instant.now());
        v.setAutor(sesion.nombre());
        return versiones.saveAndFlush(v);
    }

    private void guardarSesiones(VersionHorario v, List<Sesion> nuevas) {
        // persist directo (inserción en lote): con ids asignados, saveAll haría un SELECT por cada sesión
        for (Sesion s : nuevas) {
            s.setVersionId(v.getId());
            em.persist(s);
        }
        em.flush();
    }

    private static String recortar(String s) {
        return s.length() <= 600 ? s : s.substring(0, 599) + "…";
    }
}
