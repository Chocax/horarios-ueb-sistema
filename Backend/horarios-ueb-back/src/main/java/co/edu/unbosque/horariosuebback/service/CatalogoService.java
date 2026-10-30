package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.dto.Dtos.AulaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.BloqueDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.CursoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.CursoRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.DocenteDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ImportacionRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.OfertaDto;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.OfertaCurso;
import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import co.edu.unbosque.horariosuebback.entity.enums.TipoCurso;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.AulaRepository;
import co.edu.unbosque.horariosuebback.repository.CursoRepository;
import co.edu.unbosque.horariosuebback.repository.DocenteRepository;
import co.edu.unbosque.horariosuebback.repository.OfertaRepository;
import co.edu.unbosque.horariosuebback.repository.PeriodoRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Docentes, cursos y aulas (CU-04 a CU-07 · RF-03 a RF-06, RF-17). */
@Service
public class CatalogoService {

    private final DocenteRepository docentes;
    private final CursoRepository cursos;
    private final AulaRepository aulas;
    private final OfertaRepository ofertas;
    private final PeriodoRepository periodos;
    private final BitacoraService bitacora;
    private final SesionActual sesion;

    public CatalogoService(DocenteRepository docentes, CursoRepository cursos, AulaRepository aulas, OfertaRepository ofertas,
                           PeriodoRepository periodos, BitacoraService bitacora, SesionActual sesion) {
        this.docentes = docentes;
        this.cursos = cursos;
        this.aulas = aulas;
        this.ofertas = ofertas;
        this.periodos = periodos;
        this.bitacora = bitacora;
        this.sesion = sesion;
    }

    // ------------------------------------------------------------------ docentes (CU-04, CU-05)

    @Transactional
    public DocenteDto guardarDocente(DocenteDto d) {
        List<String> err = validarDocente(d, cursos.findAll().stream().map(Curso::getId).collect(Collectors.toSet()));
        if (!err.isEmpty()) throw new NegocioException("No se pudo guardar el docente.", err);
        Docente e = docentes.findById(d.id()).orElse(null);
        boolean existe = e != null;
        if (!existe) {
            e = new Docente();
            e.setId(d.id());
        }
        Mapeo.aplicar(d, e);
        docentes.saveAndFlush(e);
        bitacora.registrar(existe ? "Modificación de docente" : "Creación de docente", e.getNombre());
        return Mapeo.docente(e);
    }

    private List<String> validarDocente(DocenteDto d, Set<String> cursosValidos) {
        List<String> err = new ArrayList<>();
        if (!Ids.valido(d.id())) err.add("El identificador interno no es válido.");
        if (d.identificador() == null || d.identificador().isBlank()) err.add("El identificador es obligatorio.");
        if (d.nombre() == null || d.nombre().isBlank()) err.add("El nombre es obligatorio.");
        if (d.tipoVinculacion() == null) err.add("Seleccione el tipo de vinculación.");
        if (d.horasMaxDocencia() < 0 || d.horasMaxDocencia() > 60) err.add("Las horas máximas deben estar entre 0 y 60.");
        if (d.correoInstitucional() != null && !d.correoInstitucional().isBlank() && !d.correoInstitucional().matches("^\\S+@\\S+\\.\\S+$")) {
            err.add("El correo no es válido.");
        }
        docentes.findByIdentificador(d.identificador() == null ? "" : d.identificador().trim())
                .filter(x -> !x.getId().equals(d.id()))
                .ifPresent(x -> err.add("Ya existe un docente con ese identificador."));
        if (!cursosValidos.containsAll(d.habilitaciones())) err.add("Alguno de los cursos habilitados no existe.");
        err.addAll(validarBloques(d.disponibilidad()));
        return err;
    }

    private static List<String> validarBloques(List<BloqueDto> bloques) {
        List<String> err = new ArrayList<>();
        for (BloqueDto b : bloques) {
            if (b.dia() < 0 || b.dia() > 5 || b.inicio() < 0 || b.fin() > 24 || b.inicio() >= b.fin()) {
                err.add("Hay un bloque de disponibilidad no válido.");
                break;
            }
        }
        long distintos = bloques.stream().map(b -> b.dia() + "|" + b.inicio()).distinct().count();
        if (distintos != bloques.size()) err.add("Hay bloques de disponibilidad repetidos.");
        return err;
    }

    /** La registra el coordinador o el propio docente (RF-04). */
    @Transactional
    public DocenteDto actualizarDisponibilidad(String docenteId, List<BloqueDto> bloques) {
        UsuarioActual u = sesion.exigir();
        boolean propio = u.rol() == Rol.DOCENTE && docenteId.equals(u.docenteId());
        if (!propio && u.rol() != Rol.COORDINADOR) throw new AccessDeniedException("Solo el coordinador o el propio docente");
        List<String> err = validarBloques(bloques);
        if (!err.isEmpty()) throw new NegocioException("No se pudo guardar la disponibilidad.", err);
        Docente d = docentes.findById(docenteId).orElseThrow(() -> NegocioException.noEncontrado("El docente"));
        Mapeo.aplicarDisponibilidad(bloques, d);
        bitacora.registrar("Registro de disponibilidad", d.getNombre() + " (" + bloques.size() + " bloques)");
        return Mapeo.docente(d);
    }

    // ------------------------------------------------------------------ cursos (CU-06)

    @Transactional
    public CursoDto guardarCurso(CursoRequest req) {
        CursoDto c = req.curso();
        PeriodoAcademico per = req.periodoId() == null ? null : periodos.findById(req.periodoId()).orElse(null);
        List<String> err = validarCurso(c, per);
        if (!err.isEmpty()) throw new NegocioException("No se pudo guardar el curso.", err);
        Curso e = cursos.findById(c.id()).orElse(null);
        boolean existe = e != null;
        if (!existe) {
            e = new Curso();
            e.setId(c.id());
        }
        Mapeo.aplicar(c, e);
        cursos.saveAndFlush(e);
        if (req.demanda() != null && per != null) guardarOferta(per.getId(), e.getId(), req.demanda());
        bitacora.registrar(existe ? "Modificación de curso" : "Creación de curso", e.getCodigo() + " " + e.getNombre());
        return Mapeo.curso(e);
    }

    private List<String> validarCurso(CursoDto c, PeriodoAcademico per) {
        List<String> err = new ArrayList<>();
        if (!Ids.valido(c.id())) err.add("El identificador interno no es válido.");
        if (c.codigo() == null || c.codigo().isBlank() || c.nombre() == null || c.nombre().isBlank()) err.add("Código y nombre son obligatorios.");
        if (c.tipo() == null) err.add("Seleccione el tipo de curso.");
        if (c.nivel() < 1 || c.nivel() > 10) err.add("El nivel debe estar entre 1 y 10.");
        if (c.creditos() < 0 || c.creditos() > 12) err.add("Los créditos deben estar entre 0 y 12.");
        int min = per == null ? 1 : per.getSesionesMinimas();
        int max = per == null ? 4 : per.getSesionesMaximas();
        if (c.tipo() == TipoCurso.PRACTICA) {
            if (c.sesionesSemanales() < 0 || c.sesionesSemanales() > max) err.add("Las sesiones semanales deben estar entre 0 y " + max + ".");
        } else if (c.sesionesSemanales() < min || c.sesionesSemanales() > max) {
            err.add("Las sesiones semanales deben estar entre " + min + " y " + max + ".");
        }
        cursos.findByCodigo(c.codigo() == null ? "" : c.codigo().trim()).filter(x -> !x.getId().equals(c.id()))
                .ifPresent(x -> err.add("Ya existe un curso con ese código."));
        return err;
    }

    @Transactional
    public OfertaDto guardarOferta(String periodoId, String cursoId, int demanda) {
        if (demanda < 0) throw new NegocioException("La demanda no puede ser negativa.");
        if (!periodos.existsById(periodoId)) throw NegocioException.noEncontrado("El periodo");
        Curso c = cursos.findById(cursoId).orElseThrow(() -> NegocioException.noEncontrado("El curso"));
        OfertaCurso o = ofertas.findById(new OfertaCurso.Llave(periodoId, cursoId)).orElse(new OfertaCurso(periodoId, cursoId, demanda));
        o.setDemandaEstimada(demanda);
        ofertas.save(o);
        bitacora.registrar("Registro de demanda", c.getNombre() + ": " + demanda);
        return Mapeo.oferta(o);
    }

    // ------------------------------------------------------------------ aulas (CU-06)

    @Transactional
    public AulaDto guardarAula(AulaDto a) {
        List<String> err = validarAula(a);
        if (!err.isEmpty()) throw new NegocioException("No se pudo guardar el aula.", err);
        Aula e = aulas.findById(a.id()).orElse(null);
        boolean existe = e != null;
        if (!existe) {
            e = new Aula();
            e.setId(a.id());
        }
        Mapeo.aplicar(a, e);
        aulas.saveAndFlush(e);
        bitacora.registrar(existe ? "Modificación de aula" : "Creación de aula", e.getCodigo() + " (" + e.getCapacidad() + ")");
        return Mapeo.aula(e);
    }

    private List<String> validarAula(AulaDto a) {
        List<String> err = new ArrayList<>();
        if (!Ids.valido(a.id())) err.add("El identificador interno no es válido.");
        if (a.codigo() == null || a.codigo().isBlank()) err.add("El código es obligatorio.");
        if (a.capacidad() <= 0 || a.capacidad() > 1000) err.add("La capacidad debe estar entre 1 y 1000.");
        aulas.findByCodigo(a.codigo() == null ? "" : a.codigo().trim().toUpperCase()).filter(x -> !x.getId().equals(a.id()))
                .ifPresent(x -> err.add("Ya existe un aula con ese código."));
        return err;
    }

    // ------------------------------------------------------------------ importación CSV (CU-07 · RF-17): todo o nada

    @Transactional
    public void importar(String periodoId, ImportacionRequest req) {
        if (!periodos.existsById(periodoId)) throw NegocioException.noEncontrado("El periodo");
        PeriodoAcademico per = periodos.findById(periodoId).orElseThrow();
        List<String> err = new ArrayList<>();
        Set<String> cursosValidos = cursos.findAll().stream().map(Curso::getId).collect(Collectors.toSet());
        if (req.cursos() != null) req.cursos().forEach(c -> {
            cursosValidos.add(c.id());
            validarCurso(c, per).forEach(x -> err.add("Curso " + c.codigo() + ": " + x));
        });
        if (req.docentes() != null) req.docentes().forEach(d -> validarDocente(d, cursosValidos).forEach(x -> err.add("Docente " + d.identificador() + ": " + x)));
        if (req.aulas() != null) req.aulas().forEach(a -> validarAula(a).forEach(x -> err.add("Aula " + a.codigo() + ": " + x)));
        if (req.ofertas() != null) req.ofertas().forEach(o -> {
            if (!o.periodoId().equals(periodoId) || !cursosValidos.contains(o.cursoId()) || o.demandaEstimada() < 0) {
                err.add("Demanda del curso " + o.cursoId() + " no válida.");
            }
        });
        if (!err.isEmpty()) throw new NegocioException("La importación no se aplicó: hay datos no válidos.", err);

        if (req.cursos() != null) req.cursos().forEach(c -> {
            Curso e = cursos.findById(c.id()).orElseGet(() -> {
                Curso n = new Curso();
                n.setId(c.id());
                return n;
            });
            Mapeo.aplicar(c, e);
            cursos.save(e);
        });
        cursos.flush();
        if (req.docentes() != null) req.docentes().forEach(d -> {
            Docente e = docentes.findById(d.id()).orElseGet(() -> {
                Docente n = new Docente();
                n.setId(d.id());
                return n;
            });
            Mapeo.aplicar(d, e);
            docentes.save(e);
        });
        if (req.aulas() != null) req.aulas().forEach(a -> {
            Aula e = aulas.findById(a.id()).orElseGet(() -> {
                Aula n = new Aula();
                n.setId(a.id());
                return n;
            });
            Mapeo.aplicar(a, e);
            aulas.save(e);
        });
        if (req.ofertas() != null) req.ofertas().forEach(o -> ofertas.save(new OfertaCurso(periodoId, o.cursoId(), o.demandaEstimada())));
        bitacora.registrar("Importación CSV", req.resumen() == null ? "Datos importados" : req.resumen());
    }
}
