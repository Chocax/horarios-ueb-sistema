package co.edu.unbosque.horariosuebback.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** Autenticación de Spring Security cuyo principal es el {@link UsuarioActual}; la autoridad es ROLE_&lt;rol&gt;. */
public class AutenticacionUsuario extends AbstractAuthenticationToken {

    private final UsuarioActual usuario;

    public AutenticacionUsuario(UsuarioActual usuario) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
        this.usuario = usuario;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public UsuarioActual getPrincipal() {
        return usuario;
    }
}
