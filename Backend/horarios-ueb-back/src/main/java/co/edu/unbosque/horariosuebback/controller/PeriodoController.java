package co.edu.unbosque.horariosuebback.controller;

import co.edu.unbosque.horariosuebback.dto.Dtos.CrearPeriodoRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.DemandaRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.EstadoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ImportacionRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.NumeroGruposRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.OfertaDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ParametrosDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.PeriodoDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.dto.Dtos.VersionDto;
import co.edu.unbosque.horariosuebback.service.CatalogoService;
import co.edu.unbosque.horariosuebback.service.EstadoService;
import co.edu.unbosque.horariosuebback.service.GrupoService;
import co.edu.unbosque.horariosuebback.service.HorarioService;
import co.edu.unbosque.horariosuebback.service.PeriodoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estado del front, periodos, parámetros, oferta, importación y generación (CU-03, CU-07, CU-09, CU-10). */
@RestController
@RequestMapping("/api")
public class PeriodoController {

    private final EstadoService estado;
    private final PeriodoService periodos;
    private final CatalogoService catalogo;
    private final GrupoService grupos;
    private final HorarioService horario;

    public PeriodoController(EstadoService estado, PeriodoService periodos, CatalogoService catalogo, GrupoService grupos, HorarioService horario) {
        this.estado = estado;
        this.periodos = periodos;
        this.catalogo = catalogo;
        this.grupos = grupos;
        this.horario = horario;
    }

    /** Cualquier usuario autenticado; el contenido se filtra según su rol. */
    @GetMapping("/estado")
    public EstadoDto estado(@RequestParam(name = "periodo", required = false) String periodo) {
        return estado.estado(periodo);
    }

    @PostMapping("/periodos")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    public Respuesta<PeriodoDto> crear(@Valid @RequestBody CrearPeriodoRequest req) {
        return Respuesta.ok(periodos.crear(req));
    }

    @PutMapping("/periodos/{id}/parametros")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    public Respuesta<PeriodoDto> parametros(@PathVariable String id, @Valid @RequestBody ParametrosDto req) {
        return Respuesta.ok(periodos.actualizarParametros(id, req));
    }

    @PutMapping("/periodos/{id}/ofertas/{cursoId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<OfertaDto> demanda(@PathVariable String id, @PathVariable String cursoId, @Valid @RequestBody DemandaRequest req) {
        return Respuesta.ok(catalogo.guardarOferta(id, cursoId, req.demanda()));
    }

    @PostMapping("/periodos/{id}/cursos/{cursoId}/numero-grupos")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<Void> numeroGrupos(@PathVariable String id, @PathVariable String cursoId, @Valid @RequestBody NumeroGruposRequest req) {
        return Respuesta.ok(grupos.aplicarNumeroGrupos(id, cursoId, req.numero()), null);
    }

    @PostMapping("/periodos/{id}/importacion")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<Void> importar(@PathVariable String id, @Valid @RequestBody ImportacionRequest req) {
        catalogo.importar(id, req);
        return Respuesta.ok("Importación aplicada.", null);
    }

    @PostMapping("/periodos/{id}/generar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<VersionDto> generar(@PathVariable String id) {
        return Respuesta.ok(horario.generar(id));
    }
}
