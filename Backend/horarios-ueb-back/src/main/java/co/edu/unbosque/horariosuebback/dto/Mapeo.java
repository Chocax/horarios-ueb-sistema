package co.edu.unbosque.horariosuebback.dto;

import co.edu.unbosque.horariosuebback.dto.Dtos.AtencionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.AulaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.BloqueDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.CursoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.DocenteDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.GrupoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ObservacionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.OfertaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ParametrosDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.PendienteDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.PeriodoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.RangoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.SesionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.VersionDto;
import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.BloqueDisponible;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.GrupoPendiente;
import co.edu.unbosque.horariosuebback.entity.HorarioAtencion;
import co.edu.unbosque.horariosuebback.entity.Observacion;
import co.edu.unbosque.horariosuebback.entity.OfertaCurso;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.RangoHorario;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.Usuario;
import co.edu.unbosque.horariosuebback.entity.VersionHorario;
import co.edu.unbosque.horariosuebback.entity.enums.Vinculacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Conversión entre entidades y DTOs. */
public final class Mapeo {

    private Mapeo() {
    }

    public static PeriodoDto periodo(PeriodoAcademico p) {
        Map<Vinculacion, Integer> horas = new EnumMap<>(Vinculacion.class);
        horas.put(Vinculacion.TIEMPO_COMPLETO, p.getHorasTiempoCompleto());
        horas.put(Vinculacion.TRES_CUARTOS, p.getHorasTresCuartos());
        horas.put(Vinculacion.MEDIO_TIEMPO, p.getHorasMedioTiempo());
        horas.put(Vinculacion.CUARTO_TIEMPO, p.getHorasCuartoTiempo());
        ParametrosDto par = new ParametrosDto(
                new RangoDto(p.getFranjaLvInicio(), p.getFranjaLvFin()),
                new RangoDto(p.getFranjaSabInicio(), p.getFranjaSabFin()),
                p.getBloquesNoAsignables().stream().map(b -> new RangoDto(b.getInicio(), b.getFin())).toList(),
                p.getDuracionSesion(), p.getCupoMaximo(), p.getToleranciaCupo(), p.getCupoMinimo(),
                p.getSesionesMinimas(), p.getSesionesMaximas(), horas);
        return new PeriodoDto(p.getId(), p.getCodigo(), p.getAnio(), p.getSemestre(), p.getEstado(), par);
    }

    public static void aplicarParametros(ParametrosDto d, PeriodoAcademico p) {
        p.setFranjaLvInicio(d.franjaLunesViernes().inicio());
        p.setFranjaLvFin(d.franjaLunesViernes().fin());
        p.setFranjaSabInicio(d.franjaSabado().inicio());
        p.setFranjaSabFin(d.franjaSabado().fin());
        p.getBloquesNoAsignables().clear();
        d.bloquesNoAsignables().forEach(b -> p.getBloquesNoAsignables().add(new RangoHorario(b.inicio(), b.fin())));
        p.setDuracionSesion(d.duracionSesion());
        p.setCupoMaximo(d.cupoMaximo());
        p.setToleranciaCupo(d.toleranciaCupo());
        p.setCupoMinimo(d.cupoMinimo());
        p.setSesionesMinimas(d.sesionesMinimas());
        p.setSesionesMaximas(d.sesionesMaximas());
        p.setHorasTiempoCompleto(d.horasMaxPorVinculacion().getOrDefault(Vinculacion.TIEMPO_COMPLETO, 0));
        p.setHorasTresCuartos(d.horasMaxPorVinculacion().getOrDefault(Vinculacion.TRES_CUARTOS, 0));
        p.setHorasMedioTiempo(d.horasMaxPorVinculacion().getOrDefault(Vinculacion.MEDIO_TIEMPO, 0));
        p.setHorasCuartoTiempo(d.horasMaxPorVinculacion().getOrDefault(Vinculacion.CUARTO_TIEMPO, 0));
    }

    public static CursoDto curso(Curso c) {
        return new CursoDto(c.getId(), c.getCodigo(), c.getNombre(), c.getNivel(), c.getCreditos(), c.getSesionesSemanales(), c.getTipo(),
                new HashSet<>(c.getCaracteristicasRequeridas()));
    }

    public static void aplicar(CursoDto d, Curso c) {
        c.setCodigo(d.codigo().trim());
        c.setNombre(d.nombre().trim());
        c.setNivel(d.nivel());
        c.setCreditos(d.creditos());
        c.setSesionesSemanales(d.sesionesSemanales());
        c.setTipo(d.tipo());
        c.getCaracteristicasRequeridas().clear();
        c.getCaracteristicasRequeridas().addAll(d.caracteristicasRequeridas());
    }

    public static OfertaDto oferta(OfertaCurso o) {
        return new OfertaDto(o.getPeriodoId(), o.getCursoId(), o.getDemandaEstimada());
    }

    public static DocenteDto docente(Docente d) {
        return new DocenteDto(d.getId(), d.getIdentificador(), d.getNombre(), d.getCorreoInstitucional(), d.getTipoVinculacion(),
                d.getCategoriaEscalafon(), d.getHorasMaxDocencia(), new HashSet<>(d.getHabilitaciones()),
                d.getDisponibilidad().stream().map(b -> new BloqueDto(b.getDia(), b.getInicio(), b.getFin())).toList());
    }

    public static void aplicar(DocenteDto d, Docente e) {
        e.setIdentificador(d.identificador().trim());
        e.setNombre(d.nombre().trim());
        e.setCorreoInstitucional(d.correoInstitucional() == null ? "" : d.correoInstitucional().trim());
        e.setTipoVinculacion(d.tipoVinculacion());
        e.setCategoriaEscalafon(d.categoriaEscalafon() == null ? "" : d.categoriaEscalafon().trim());
        e.setHorasMaxDocencia(d.horasMaxDocencia());
        e.getHabilitaciones().clear();
        e.getHabilitaciones().addAll(d.habilitaciones());
        aplicarDisponibilidad(d.disponibilidad(), e);
    }

    public static void aplicarDisponibilidad(List<BloqueDto> bloques, Docente e) {
        List<BloqueDisponible> nuevos = new ArrayList<>(bloques.stream().map(b -> new BloqueDisponible(b.dia(), b.inicio(), b.fin())).toList());
        nuevos.sort(Comparator.comparingInt(BloqueDisponible::getDia).thenComparingInt(BloqueDisponible::getInicio));
        e.getDisponibilidad().clear();
        e.getDisponibilidad().addAll(nuevos);
    }

    public static AulaDto aula(Aula a) {
        return new AulaDto(a.getId(), a.getCodigo(), a.getNombre(), a.getCapacidad(), new HashSet<>(a.getCaracteristicas()), a.getBloque(), a.getPiso());
    }

    public static void aplicar(AulaDto d, Aula a) {
        a.setCodigo(d.codigo().trim().toUpperCase());
        a.setNombre(d.nombre() == null || d.nombre().isBlank() ? "Salón " + a.getCodigo() : d.nombre().trim());
        a.setCapacidad(d.capacidad());
        a.setBloque(d.bloque());
        a.setPiso(d.piso());
        a.getCaracteristicas().clear();
        a.getCaracteristicas().addAll(d.caracteristicas());
    }

    public static GrupoDto grupo(Grupo g) {
        return new GrupoDto(g.getId(), g.getPeriodoId(), g.getCursoId(), g.getCodigo(), g.getJornada(), g.getCupoPlaneado(), g.getInscritos(),
                g.getEstado(), g.getDocentePreferido());
    }

    public static SesionDto sesion(Sesion s) {
        return new SesionDto(s.getId(), s.getGrupoId(), s.getDia(), s.getInicio(), s.getDocenteId(), s.getAulaId());
    }

    public static PendienteDto pendiente(GrupoPendiente p) {
        return new PendienteDto(p.getGrupoId(), p.getCausa(), p.getRegla());
    }

    public static VersionDto version(VersionHorario v, List<Sesion> sesiones, List<GrupoPendiente> pendientes) {
        return new VersionDto(v.getId(), v.getPeriodoId(), v.getNumero(), v.getEstado(), v.getOrigen(), v.getFechaCreacion(),
                v.getFechaPublicacion(), v.getAutor(), sesiones.stream().map(Mapeo::sesion).toList(), pendientes.stream().map(Mapeo::pendiente).toList());
    }

    public static ObservacionDto observacion(Observacion o) {
        return new ObservacionDto(o.getId(), o.getVersionId(), o.getUsuarioId(), o.getAutor(), o.getConcepto(), o.getTexto(), o.getFecha());
    }

    public static UsuarioDto usuario(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.isActivo(), u.getDocenteId());
    }

    public static AtencionDto atencion(HorarioAtencion h) {
        return new AtencionDto(h.getId(), h.getPeriodoId(), h.getDocenteId(), h.getCursoId(), h.getModo(), h.getDia(), h.getInicio(), h.getFin(), h.getLugar());
    }
}
