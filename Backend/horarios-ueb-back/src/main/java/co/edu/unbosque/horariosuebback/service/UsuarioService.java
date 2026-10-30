package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioDto;
import co.edu.unbosque.horariosuebback.dto.Dtos.UsuarioRequest;
import co.edu.unbosque.horariosuebback.dto.Mapeo;
import co.edu.unbosque.horariosuebback.entity.Usuario;
import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.repository.DocenteRepository;
import co.edu.unbosque.horariosuebback.repository.UsuarioRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Usuarios y roles (CU-02 · RF-22). Solo el administrador. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final DocenteRepository docentes;
    private final PasswordEncoder encoder;
    private final BitacoraService bitacora;
    private final SesionActual sesion;

    public UsuarioService(UsuarioRepository usuarios, DocenteRepository docentes, PasswordEncoder encoder, BitacoraService bitacora, SesionActual sesion) {
        this.usuarios = usuarios;
        this.docentes = docentes;
        this.encoder = encoder;
        this.bitacora = bitacora;
        this.sesion = sesion;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getRol).thenComparing(Usuario::getNombre))
                .map(Mapeo::usuario).toList();
    }

    @Transactional
    public UsuarioDto guardar(UsuarioRequest r) {
        UsuarioActual yo = sesion.exigir(Rol.ADMINISTRADOR);
        Usuario existente = r.id() == null ? null : usuarios.findById(r.id()).orElse(null);
        String correo = r.correo().trim().toLowerCase();
        List<String> err = new ArrayList<>();
        if (r.nombre().isBlank()) err.add("El nombre es obligatorio.");
        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) err.add("El correo no es válido.");
        usuarios.findByCorreoIgnoreCase(correo).filter(x -> existente == null || !x.getId().equals(existente.getId()))
                .ifPresent(x -> err.add("Ya existe un usuario con ese correo."));
        if (existente == null && (r.clave() == null || r.clave().isEmpty())) err.add("Asigne una contraseña inicial.");
        if (r.clave() != null && !r.clave().isEmpty()) err.addAll(AuthService.validarClave(r.clave()));
        if (r.rol() == Rol.DOCENTE && r.docenteId() != null && !docentes.existsById(r.docenteId())) err.add("El docente vinculado no existe.");
        if (existente != null && existente.getId().equals(yo.id()) && (r.rol() != existente.getRol() || !r.activo())) {
            err.add("No puede cambiar su propio rol ni desactivarse.");
        }
        if (!err.isEmpty()) throw new NegocioException("No se pudo guardar el usuario.", err);

        Usuario u = existente != null ? existente : new Usuario();
        if (existente == null) u.setId(r.id() != null && Ids.valido(r.id()) && !usuarios.existsById(r.id()) ? r.id() : Ids.nuevo("usr"));
        List<String> detalle = new ArrayList<>();
        if (existente == null) detalle.add("rol " + r.rol());
        else {
            if (existente.getRol() != r.rol()) detalle.add("rol " + existente.getRol() + " → " + r.rol());
            if (existente.isActivo() != r.activo()) detalle.add(r.activo() ? "activado" : "desactivado");
            if (r.clave() != null && !r.clave().isEmpty()) detalle.add("contraseña restablecida");
        }
        u.setNombre(r.nombre().trim());
        u.setCorreo(correo);
        u.setRol(r.rol());
        u.setActivo(r.activo());
        u.setDocenteId(r.rol() == Rol.DOCENTE ? r.docenteId() : null);
        if (r.clave() != null && !r.clave().isEmpty()) u.setHashClave(encoder.encode(r.clave()));
        usuarios.saveAndFlush(u);
        bitacora.registrar(existente != null ? "Modificación de usuario" : "Creación de usuario",
                u.getNombre() + (detalle.isEmpty() ? "" : " (" + String.join(", ", detalle) + ")"));
        return Mapeo.usuario(u);
    }
}
