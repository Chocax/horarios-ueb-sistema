package co.edu.unbosque.horariosuebback.controller;

import co.edu.unbosque.horariosuebback.dto.Dtos.BitacoraDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioRequest;
import co.edu.unbosque.horariosuebback.service.BitacoraService;
import co.edu.unbosque.horariosuebback.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Usuarios y roles (CU-02) y bitácora de auditoría (RF-15). */
@RestController
@RequestMapping("/api")
public class AdminController {

    private final UsuarioService usuarios;
    private final BitacoraService bitacora;

    public AdminController(UsuarioService usuarios, BitacoraService bitacora) {
        this.usuarios = usuarios;
        this.bitacora = bitacora;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<UsuarioDto> usuarios() {
        return usuarios.listar();
    }

    @PutMapping("/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Respuesta<UsuarioDto> guardar(@Valid @RequestBody UsuarioRequest req) {
        return Respuesta.ok(usuarios.guardar(req));
    }

    @GetMapping("/bitacora")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    public List<BitacoraDto> bitacora(@RequestParam(name = "limite", defaultValue = "500") int limite) {
        return bitacora.ultimos(limite);
    }
}
