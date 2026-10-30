package co.edu.unbosque.horariosuebback.controller;

import co.edu.unbosque.horariosuebback.dto.Dtos.AtencionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.AulaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.CursoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.CursoRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.DisponibilidadRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.DocenteDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.GrupoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.InscritosRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.service.AtencionService;
import co.edu.unbosque.horariosuebback.service.CatalogoService;
import co.edu.unbosque.horariosuebback.service.GrupoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Docentes, disponibilidad, cursos, aulas, grupos y horario de atención (CU-04 a CU-06, CU-08, CU-17, CU-21). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogo;
    private final GrupoService grupos;
    private final AtencionService atencion;

    public CatalogoController(CatalogoService catalogo, GrupoService grupos, AtencionService atencion) {
        this.catalogo = catalogo;
        this.grupos = grupos;
        this.atencion = atencion;
    }

    @PutMapping("/docentes/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<DocenteDto> docente(@PathVariable String id, @Valid @RequestBody DocenteDto req) {
        mismoId(id, req.id());
        return Respuesta.ok(catalogo.guardarDocente(req));
    }

    /** Coordinador o el propio docente (se verifica en el servicio). */
    @PutMapping("/docentes/{id}/disponibilidad")
    @PreAuthorize("hasAnyRole('COORDINADOR','DOCENTE')")
    public Respuesta<DocenteDto> disponibilidad(@PathVariable String id, @Valid @RequestBody DisponibilidadRequest req) {
        return Respuesta.ok(catalogo.actualizarDisponibilidad(id, req.bloques()));
    }

    @PutMapping("/cursos/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<CursoDto> curso(@PathVariable String id, @Valid @RequestBody CursoRequest req) {
        mismoId(id, req.curso().id());
        return Respuesta.ok(catalogo.guardarCurso(req));
    }

    @PutMapping("/aulas/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<AulaDto> aula(@PathVariable String id, @Valid @RequestBody AulaDto req) {
        mismoId(id, req.id());
        return Respuesta.ok(catalogo.guardarAula(req));
    }

    @PutMapping("/grupos/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<GrupoDto> grupo(@PathVariable String id, @Valid @RequestBody GrupoDto req) {
        mismoId(id, req.id());
        return Respuesta.ok(grupos.guardarGrupo(req));
    }

    @PutMapping("/grupos/{id}/inscritos")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<GrupoDto> inscritos(@PathVariable String id, @Valid @RequestBody InscritosRequest req) {
        return Respuesta.ok(grupos.registrarInscritos(id, req.inscritos()));
    }

    @PostMapping("/grupos/{id}/cerrar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<Void> cerrar(@PathVariable String id) {
        return Respuesta.ok(grupos.cerrarGrupo(id), null);
    }

    /** Coordinador o el propio docente (se verifica en el servicio). */
    @PutMapping("/atencion")
    @PreAuthorize("hasAnyRole('COORDINADOR','DOCENTE')")
    public Respuesta<AtencionDto> guardarAtencion(@Valid @RequestBody AtencionDto req) {
        return Respuesta.ok(atencion.guardar(req));
    }

    @DeleteMapping("/atencion/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','DOCENTE')")
    public Respuesta<Void> eliminarAtencion(@PathVariable String id) {
        atencion.eliminar(id);
        return Respuesta.ok(null);
    }

    private static void mismoId(String ruta, String cuerpo) {
        if (!ruta.equals(cuerpo)) throw new NegocioException("El identificador de la ruta no coincide con el de los datos.");
    }
}
