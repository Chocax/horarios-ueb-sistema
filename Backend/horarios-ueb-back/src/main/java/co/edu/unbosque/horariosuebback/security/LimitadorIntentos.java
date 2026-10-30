package co.edu.unbosque.horariosuebback.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra ataques de fuerza bruta al login: tras 5 intentos fallidos con el mismo correo (o 20 desde la misma IP)
 * se bloquea el acceso durante 15 minutos. Vive en memoria: se reinicia si el servidor se reinicia.
 */
@Component
public class LimitadorIntentos {

    private static final int MAX_POR_CORREO = 5;
    private static final int MAX_POR_IP = 20;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant desde) {
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public boolean bloqueado(String correo, String ip) {
        return superado("c:" + correo, MAX_POR_CORREO) || superado("i:" + ip, MAX_POR_IP);
    }

    public void fallo(String correo, String ip) {
        sumar("c:" + correo);
        sumar("i:" + ip);
        if (registros.size() > 10_000) limpiar();
    }

    public void exito(String correo) {
        registros.remove("c:" + correo);
    }

    private boolean superado(String clave, int max) {
        Registro r = registros.get(clave);
        if (r == null) return false;
        if (r.desde().plus(BLOQUEO).isBefore(Instant.now())) {
            registros.remove(clave);
            return false;
        }
        return r.fallos() >= max;
    }

    private void sumar(String clave) {
        registros.merge(clave, new Registro(1, Instant.now()), (a, b) -> new Registro(a.fallos() + 1, a.desde()));
    }

    private void limpiar() {
        Instant limite = Instant.now().minus(BLOQUEO);
        registros.entrySet().removeIf(e -> e.getValue().desde().isBefore(limite));
    }
}
