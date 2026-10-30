package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.dto.Dtos.CrearPeriodoRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.ParametrosDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.PeriodoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.RangoDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.OfertaCurso;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.RangoHorario;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoPeriodo;
import co.edu.unbosque.horariosuebback.entity.enums.Vinculacion;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.GrupoRepository;
import co.edu.unbosque.horariosuebback.repository.OfertaRepository;
import co.edu.unbosque.horariosuebback.repository.PeriodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Periodos académicos y sus parámetros (CU-03 · RF-01, RF-02, RF-16). */
@Service
public class PeriodoService {

    private final PeriodoRepository periodos;
    private final OfertaRepository ofertas;
    private final GrupoRepository grupos;
    private final BitacoraService bitacora;

    public PeriodoService(PeriodoRepository periodos, OfertaRepository ofertas, GrupoRepository grupos, BitacoraService bitacora) {
        this.periodos = periodos;
        this.ofertas = ofertas;
        this.grupos = grupos;
        this.bitacora = bitacora;
    }

    @Transactional
    public PeriodoDto crear(CrearPeriodoRequest req) {
        String codigo = req.anio() + "-" + req.semestre();
        if (periodos.existsByCodigo(codigo)) throw new NegocioException("El periodo " + codigo + " ya existe.");
        PeriodoAcademico base = req.basePeriodoId() == null ? null : periodos.findById(req.basePeriodoId()).orElse(null);
        PeriodoAcademico p = new PeriodoAcademico();
        p.setId("per-" + codigo);
        p.setCodigo(codigo);
        p.setAnio(req.anio());
        p.setSemestre(req.semestre());
        p.setEstado(EstadoPeriodo.ABIERTO);
        Mapeo.aplicarParametros(base != null ? Mapeo.periodo(base).parametros() : porDefecto(), p);
        periodos.saveAndFlush(p);
        if (req.copiarOferta() && base != null) {
            ofertas.saveAll(ofertas.findByPeriodoId(base.getId()).stream()
                    .map(o -> new OfertaCurso(p.getId(), o.getCursoId(), o.getDemandaEstimada())).toList());
            String de = base.getId().replace("per-", "");
            String a = p.getId().replace("per-", "");
            List<Grupo> copia = new ArrayList<>();
            for (Grupo g : grupos.findByPeriodoIdAndEstado(base.getId(), EstadoGrupo.ABIERTO)) {
                Grupo n = new Grupo();
                n.setId(g.getId().contains(de) ? g.getId().replace(de, a) : Ids.nuevo("grp"));
                n.setPeriodoId(p.getId());
                n.setCursoId(g.getCursoId());
                n.setCodigo(g.getCodigo());
                n.setJornada(g.getJornada());
                n.setCupoPlaneado(g.getCupoPlaneado());
                n.setEstado(EstadoGrupo.ABIERTO);
                n.setDocentePreferido(g.getDocentePreferido());
                copia.add(n);
            }
            grupos.saveAll(copia);
        }
        bitacora.registrar("Creación de periodo", codigo + (req.copiarOferta() && base != null ? " (oferta copiada de " + base.getCodigo() + ")" : ""));
        return Mapeo.periodo(p);
    }

    @Transactional
    public PeriodoDto actualizarParametros(String periodoId, ParametrosDto dto) {
        List<String> errores = validarParametros(dto);
        if (!errores.isEmpty()) throw new NegocioException("Revise los parámetros.", errores);
        PeriodoAcademico p = periodos.findById(periodoId).orElseThrow(() -> NegocioException.noEncontrado("El periodo"));
        Mapeo.aplicarParametros(dto, p);
        bitacora.registrar("Modificación de parámetros", "Periodo " + p.getCodigo());
        return Mapeo.periodo(p);
    }

    public static ParametrosDto porDefecto() {
        java.util.Map<Vinculacion, Integer> horas = new java.util.EnumMap<>(Vinculacion.class);
        horas.put(Vinculacion.TIEMPO_COMPLETO, 16);
        horas.put(Vinculacion.TRES_CUARTOS, 12);
        horas.put(Vinculacion.MEDIO_TIEMPO, 8);
        horas.put(Vinculacion.CUARTO_TIEMPO, 4);
        return new ParametrosDto(new RangoDto(7, 22), new RangoDto(7, 13), List.of(new RangoDto(13, 14)), 2, 40, 10, 10, 1, 4, horas);
    }

    public static List<String> validarParametros(ParametrosDto p) {
        List<String> err = new ArrayList<>();
        if (!rangoOk(p.franjaLunesViernes())) err.add("La franja de lunes a viernes no es válida.");
        if (!rangoOk(p.franjaSabado())) err.add("La franja del sábado no es válida.");
        for (int i = 0; i < p.bloquesNoAsignables().size(); i++) {
            if (!rangoOk(p.bloquesNoAsignables().get(i))) err.add("El bloque no asignable " + (i + 1) + " no es válido.");
        }
        if (p.duracionSesion() < 1 || p.duracionSesion() > 4) err.add("La duración de la sesión debe estar entre 1 y 4 horas.");
        if (p.cupoMaximo() <= 0 || p.cupoMaximo() > 500) err.add("El cupo máximo debe estar entre 1 y 500.");
        if (p.toleranciaCupo() < 0 || p.toleranciaCupo() > 50) err.add("La tolerancia debe estar entre 0 % y 50 %.");
        if (p.cupoMinimo() < 0 || p.cupoMinimo() >= p.cupoMaximo()) err.add("El cupo mínimo debe ser menor que el cupo máximo.");
        if (p.sesionesMinimas() < 1 || p.sesionesMinimas() > p.sesionesMaximas()) err.add("Las sesiones mínimas deben ser al menos 1 y no superar las máximas.");
        if (p.sesionesMaximas() > 6) err.add("Las sesiones máximas no pueden superar 6 (una por día).");
        for (Vinculacion v : Vinculacion.values()) {
            Integer h = p.horasMaxPorVinculacion().get(v);
            if (h == null || h < 0 || h > 60) err.add("Las horas máximas de " + v + " no son válidas.");
        }
        return err;
    }

    private static boolean rangoOk(RangoDto r) {
        return r != null && r.inicio() >= 0 && r.fin() <= 24 && r.inicio() < r.fin();
    }

    static List<RangoHorario> rangos(List<RangoDto> l) {
        return l.stream().map(r -> new RangoHorario(r.inicio(), r.fin())).toList();
    }
}
