package co.edu.unbosque.horariosuebback.controller;

import co.edu.unbosque.horariosuebback.dto.Dtos.MoverSesionRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.ObservacionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.ObservacionRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.dto.Dtos.SesionDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.UbicarRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.VersionDto;
import co.edu.unbosque.horariosuebback.service.HorarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ajuste del tablero, publicación, restauración y concepto del comité (CU-11 a CU-15, CU-20). */
@RestController
@RequestMapping("/api/versiones")
public class VersionController {

    private final HorarioService horario;

    public VersionController(HorarioService horario) {
        this.horario = horario;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','COMITE','ADMINISTRADOR')")
    public VersionDto version(@PathVariable String id) {
        return horario.version(id);
    }

    @PatchMapping("/{id}/sesiones/{sesionId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<SesionDto> mover(@PathVariable String id, @PathVariable String sesionId,
                                                                               @RequestBody MoverSesionRequest req) {
        return Respuesta.ok(horario.mover(id, sesionId, req));
    }

    @PostMapping("/{id}/pendientes/{grupoId}/ubicar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<VersionDto> ubicar(@PathVariable String id, @PathVariable String grupoId, @RequestBody UbicarRequest req) {
        return horario.ubicarPendiente(id, grupoId, req.dia(), req.inicio());
    }

    @PostMapping("/{id}/pendientes/{grupoId}/reintentar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<VersionDto> reintentar(@PathVariable String id, @PathVariable String grupoId) {
        return horario.reintentarPendiente(id, grupoId);
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<VersionDto> publicar(@PathVariable String id) {
        return Respuesta.ok(horario.publicar(id));
    }

    @PostMapping("/{id}/restaurar")
    @PreAuthorize("hasRole('COORDINADOR')")
    public Respuesta<VersionDto> restaurar(@PathVariable String id) {
        return Respuesta.ok(horario.restaurar(id));
    }

    /** Solo el administrador: borra una versión de prueba como si nunca hubiera existido. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Respuesta<Void> eliminar(@PathVariable String id) {
        horario.eliminarVersion(id);
        return Respuesta.ok("Versión eliminada.", null);
    }

    @PostMapping("/{id}/observaciones")
    @PreAuthorize("hasRole('COMITE')")
    public Respuesta<ObservacionDto> observacion(@PathVariable String id, @Valid @RequestBody ObservacionRequest req) {
        return Respuesta.ok(horario.registrarObservacion(id, req.concepto(), req.texto()));
    }
}
