package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.domain.Textos;
import co.edu.unbosque.horariosuebback.dto.Dtos.AtencionDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.BloqueDisponible;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.HorarioAtencion;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.ModoAtencion;
import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.AtencionRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Horario de atención por materia (CU-21 · RF-23): lo registra el docente o el coordinador en su nombre. */
@Service
public class AtencionService {

    private final AtencionRepository repo;
    private final CargadorPeriodo cargador;
    private final BitacoraService bitacora;
    private final SesionActual sesion;

    public AtencionService(AtencionRepository repo, CargadorPeriodo cargador, BitacoraService bitacora, SesionActual sesion) {
        this.repo = repo;
        this.cargador = cargador;
        this.bitacora = bitacora;
        this.sesion = sesion;
    }

    @Transactional
    public AtencionDto guardar(AtencionDto h) {
        exigirPropioOCoordinador(h.docenteId());
        CargadorPeriodo.Datos d = cargador.cargar(h.periodoId());
        Catalogo cat = d.catalogo();
        Docente doc = cat.docente(h.docenteId());
        if (doc == null) throw NegocioException.noEncontrado("El docente");
        if (cat.curso(h.cursoId()) == null) throw NegocioException.noEncontrado("El curso");
        HorarioAtencion e = repo.findByPeriodoIdAndDocenteIdAndCursoId(h.periodoId(), h.docenteId(), h.cursoId()).orElse(null);
        List<String> err = validar(h, e == null ? null : e.getId(), d);
        if (!err.isEmpty()) throw new NegocioException("No se puede guardar el horario de atención", err);
        if (e == null) {
            e = new HorarioAtencion();
            e.setId(Ids.valido(h.id()) && !repo.existsById(h.id()) ? h.id() : Ids.nuevo("ate"));
            e.setPeriodoId(h.periodoId());
            e.setDocenteId(h.docenteId());
            e.setCursoId(h.cursoId());
        }
        boolean fijo = h.modo() == ModoAtencion.HORARIO;
        e.setModo(h.modo());
        e.setDia(fijo ? h.dia() : null);
        e.setInicio(fijo ? h.inicio() : null);
        e.setFin(fijo ? h.fin() : null);
        e.setLugar(h.lugar() == null ? "" : h.lugar().trim());
        repo.save(e);
        bitacora.registrar("Horario de atención", doc.getNombre() + " · " + cat.curso(h.cursoId()).getNombre() + ": " + texto(e));
        return Mapeo.atencion(e);
    }

    @Transactional
    public void eliminar(String id) {
        HorarioAtencion e = repo.findById(id).orElseThrow(() -> NegocioException.noEncontrado("El horario de atención"));
        exigirPropioOCoordinador(e.getDocenteId());
        repo.delete(e);
        bitacora.registrar("Horario de atención", "Se quitó el horario de atención (" + e.getDocenteId() + " · " + e.getCursoId() + ")");
    }

    private void exigirPropioOCoordinador(String docenteId) {
        UsuarioActual u = sesion.exigir();
        boolean propio = u.rol() == Rol.DOCENTE && docenteId.equals(u.docenteId());
        if (!propio && u.rol() != Rol.COORDINADOR) throw new AccessDeniedException("Solo el coordinador o el propio docente");
    }

    /** Reglas: 1 o 2 horas, dentro de la franja, fuera del almuerzo, en su disponibilidad y sin cruzar sus clases. */
    private List<String> validar(AtencionDto h, String idActual, CargadorPeriodo.Datos d) {
        if (h.modo() == ModoAtencion.ACORDAR) return List.of();
        if (h.dia() == null || h.inicio() == null || h.fin() == null) return List.of("Elija día, hora de inicio y duración.");
        List<String> err = new ArrayList<>();
        Parametros p = d.parametros();
        int dia = h.dia(), ini = h.inicio(), fin = h.fin();
        if (dia < 0 || dia > 5) return List.of("El día no es válido.");
        int dur = fin - ini;
        if (dur < 1 || dur > 2) err.add("El horario de atención es de 1 o 2 horas a la semana.");
        Parametros.Rango franja = p.franja(dia);
        if (ini < franja.inicio() || fin > franja.fin()) err.add("Debe quedar dentro de la franja hábil (" + Textos.rango(franja.inicio(), franja.fin()) + ").");
        p.bloquesNoAsignables().stream().filter(b -> b.seSolapaCon(ini, fin)).findFirst()
                .ifPresent(b -> err.add("Cruza el bloque de almuerzo " + Textos.rango(b.inicio(), b.fin()) + "."));
        Docente doc = d.catalogo().docente(h.docenteId());
        boolean disponible = false;
        for (BloqueDisponible b : doc.getDisponibilidad()) {
            if (b.getDia() == dia && b.getInicio() <= ini && b.getFin() >= fin) disponible = true;
        }
        if (!disponible) err.add(doc.getNombre() + " no está disponible el " + Textos.dia(dia) + " " + Textos.rango(ini, fin) + ".");
        int durSesion = p.duracionSesion();
        for (Sesion s : cargador.sesionesVigentes(h.periodoId())) {
            if (h.docenteId().equals(s.getDocenteId()) && s.getDia() == dia && s.getInicio() < fin && ini < s.getInicio() + durSesion) {
                err.add("Se cruza con su clase de " + d.catalogo().etiquetaGrupo(s.getGrupoId()) + " (" + Textos.dia(s.getDia()) + " "
                        + Textos.rango(s.getInicio(), s.getInicio() + durSesion) + ").");
                break;
            }
        }
        repo.findByPeriodoIdAndDocenteId(h.periodoId(), h.docenteId()).stream()
                .filter(x -> !x.getId().equals(idActual) && !x.getCursoId().equals(h.cursoId()) && x.getModo() == ModoAtencion.HORARIO
                        && x.getDia() != null && x.getDia() == dia && x.getInicio() < fin && ini < x.getFin())
                .findFirst()
                .ifPresent(x -> err.add("Se cruza con su horario de atención de " + d.catalogo().curso(x.getCursoId()).getNombre() + "."));
        return err;
    }

    private static String texto(HorarioAtencion h) {
        if (h.getModo() == ModoAtencion.ACORDAR) return "Acordar con el docente";
        return Textos.dia(h.getDia()) + " " + Textos.rango(h.getInicio(), h.getFin());
    }
}
