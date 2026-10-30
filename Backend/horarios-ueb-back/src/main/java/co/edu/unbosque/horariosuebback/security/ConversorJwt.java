package co.edu.unbosque.horariosuebback.security;

import co.edu.unbosque.horariosuebback.entity.Usuario;
import co.edu.unbosque.horariosuebback.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte el JWT ya verificado en el usuario de la petición. Consulta la base de datos en cada petición para que
 * desactivar un usuario o cambiarle el rol tenga efecto inmediato (RF-22), aunque su token siga vigente.
 */
@Component
public class ConversorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarios;

    public ConversorJwt(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario u = usuarios.findById(jwt.getSubject())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new DisabledException("Usuario inexistente o desactivado"));
        return new AutenticacionUsuario(new UsuarioActual(u.getId(), u.getNombre(), u.getRol(), u.getDocenteId()));
    }
}
