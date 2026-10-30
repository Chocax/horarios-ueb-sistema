package co.edu.unbosque.horariosuebback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración propia de la aplicación (prefijo «app» en application.properties / variables de entorno).
 *
 * @param jwtSecreto      secreto HMAC para firmar los tokens (mínimo 32 caracteres). En producción: variable JWT_SECRET.
 * @param jwtHoras        vigencia del token en horas
 * @param corsOrigenes    orígenes del front que pueden llamar a la API (p. ej. https://horarios-ueb.vercel.app)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String jwtSecreto, int jwtHoras, List<String> corsOrigenes) {
}
