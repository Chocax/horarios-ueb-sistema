package co.edu.unbosque.horariosuebback.controller;

import co.edu.unbosque.horariosuebback.dto.Dtos.CambioClaveRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.LoginRequest;
import co.edu.unbosque.horariosuebback.dto.Dtos.LoginResponse;
import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Inicio de sesión (CU-01), cambio de contraseña y verificación de salud del servicio. */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;
    private final JdbcTemplate jdbc;

    public AuthController(AuthService auth, JdbcTemplate jdbc) {
        this.auth = auth;
        this.jdbc = jdbc;
    }

    @PostMapping("/auth/login")
    public Respuesta<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return Respuesta.ok(auth.login(req.correo(), req.clave(), http.getRemoteAddr()));
    }

    @PostMapping("/auth/cambiar-clave")
    public Respuesta<Void> cambiarClave(@Valid @RequestBody CambioClaveRequest req) {
        auth.cambiarClave(req.claveActual(), req.claveNueva());
        return Respuesta.ok("Contraseña actualizada.", null);
    }

    /** Público: lo usan Render (health check) y el front para «despertar» el servidor. Consulta la base de datos. */
    @GetMapping("/salud")
    public Map<String, String> salud() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("estado", "ok");
    }
}
