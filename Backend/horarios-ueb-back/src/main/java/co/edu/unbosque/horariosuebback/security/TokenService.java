package co.edu.unbosque.horariosuebback.security;

import co.edu.unbosque.horariosuebback.config.AppProperties;
import co.edu.unbosque.horariosuebback.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite el token de acceso (JWT firmado con HS256). Solo lleva el id del usuario: rol y estado se leen de la BD. */
@Service
public class TokenService {

    public static final String EMISOR = "horarios-ueb";

    private final JwtEncoder encoder;
    private final Duration vigencia;

    public TokenService(JwtEncoder encoder, AppProperties props) {
        this.encoder = encoder;
        this.vigencia = Duration.ofHours(props.jwtHoras() > 0 ? props.jwtHoras() : 8);
    }

    public String emitir(Usuario u) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(u.getId())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public long segundosVigencia() {
        return vigencia.toSeconds();
    }
}
