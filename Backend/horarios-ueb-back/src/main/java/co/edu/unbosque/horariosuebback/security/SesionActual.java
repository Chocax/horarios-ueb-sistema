package co.edu.unbosque.horariosuebback.security;

import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Acceso al usuario autenticado desde los servicios. */
@Component
public class SesionActual {

    public UsuarioActual usuario() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof AutenticacionUsuario au) return au.getPrincipal();
        return null;
    }

    public UsuarioActual exigir() {
        UsuarioActual u = usuario();
        if (u == null) throw new AccessDeniedException("Sin sesión");
        return u;
    }

    /** Lanza AccessDeniedException si el usuario no tiene alguno de los roles (la bitácora lo registra). */
    public UsuarioActual exigir(Rol... roles) {
        UsuarioActual u = exigir();
        if (!u.es(roles)) throw new AccessDeniedException("Acción no permitida para el rol " + u.rol());
        return u;
    }

    public String nombre() {
        UsuarioActual u = usuario();
        return u == null ? "Sistema" : u.nombre();
    }

    public String rol() {
        UsuarioActual u = usuario();
        return u == null ? "SISTEMA" : u.rol().name();
    }
}
