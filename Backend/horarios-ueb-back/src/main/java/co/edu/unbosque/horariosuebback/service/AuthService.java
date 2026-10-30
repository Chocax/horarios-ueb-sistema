package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.dto.Dtos.LoginResponse;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Usuario;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.UsuarioRepository;
import co.edu.unbosque.horariosuebback.security.LimitadorIntentos;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.TokenService;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Inicio de sesión (CU-01) y cambio de contraseña. */
@Service
public class AuthService {

    /** Hash de una clave cualquiera: se compara cuando el correo no existe para que el tiempo de respuesta no lo delate. */
    private static final String HASH_SENUELO = "$2a$10$.BXfgP/AXuNhLMQ9HoHIOeeJ3nC8yp3j4ieOZq55t4lwlL4.9FZce";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LimitadorIntentos limitador;
    private final BitacoraService bitacora;
    private final SesionActual sesion;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, TokenService tokens, LimitadorIntentos limitador,
                       BitacoraService bitacora, SesionActual sesion) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.limitador = limitador;
        this.bitacora = bitacora;
        this.sesion = sesion;
    }

    @Transactional
    public LoginResponse login(String correo, String clave, String ip) {
        String c = correo.trim().toLowerCase();
        if (limitador.bloqueado(c, ip)) {
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo.", null, null);
        }
        Usuario u = usuarios.findByCorreoIgnoreCase(c).orElse(null);
        boolean claveOk = encoder.matches(clave, u == null ? HASH_SENUELO : u.getHashClave());
        if (u == null || !claveOk) {
            limitador.fallo(c, ip);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.", null, null);
        }
        if (!u.isActivo()) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "El usuario está desactivado. Comuníquese con el administrador.", null, null);
        }
        limitador.exito(c);
        bitacora.registrarAparte(u.getNombre(), u.getRol().name(), "Inicio de sesión", u.getNombre() + " (" + u.getCorreo() + ")");
        return new LoginResponse(tokens.emitir(u), tokens.segundosVigencia(), Mapeo.usuario(u));
    }

    @Transactional
    public void cambiarClave(String actual, String nueva) {
        UsuarioActual yo = sesion.exigir();
        Usuario u = usuarios.findById(yo.id()).orElseThrow(() -> NegocioException.noEncontrado("El usuario"));
        if (!encoder.matches(actual, u.getHashClave())) {
            throw new NegocioException("La contraseña actual no es correcta.");
        }
        List<String> errores = validarClave(nueva);
        if (encoder.matches(nueva, u.getHashClave())) errores.add("La nueva contraseña debe ser distinta de la actual.");
        if (!errores.isEmpty()) throw new NegocioException("La nueva contraseña no cumple la política.", errores);
        u.setHashClave(encoder.encode(nueva));
        bitacora.registrar("Cambio de contraseña", u.getNombre());
    }

    /** Política: 8 a 128 caracteres, con al menos una letra y un número. */
    public static List<String> validarClave(String clave) {
        List<String> err = new ArrayList<>();
        if (clave == null || clave.length() < 8) err.add("Debe tener al menos 8 caracteres.");
        if (clave != null && clave.length() > 128) err.add("No puede superar 128 caracteres.");
        if (clave != null && !clave.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*")) err.add("Debe incluir al menos una letra.");
        if (clave != null && !clave.matches(".*\\d.*")) err.add("Debe incluir al menos un número.");
        return err;
    }
}
