package co.edu.unbosque.horariosuebback.exception;

import co.edu.unbosque.horariosuebback.domain.reglas.Violacion;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/** Operación rechazada por una regla del negocio; el mensaje y los detalles se muestran al usuario. */
@Getter
public class NegocioException extends RuntimeException {

    private final HttpStatus estado;
    private final List<String> errores;
    private final List<Violacion> violaciones;

    public NegocioException(String mensaje) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, mensaje, null, null);
    }

    public NegocioException(String mensaje, List<String> errores) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, mensaje, errores, null);
    }

    public NegocioException(HttpStatus estado, String mensaje, List<String> errores, List<Violacion> violaciones) {
        super(mensaje);
        this.estado = estado;
        this.errores = errores;
        this.violaciones = violaciones;
    }

    public static NegocioException noEncontrado(String que) {
        return new NegocioException(HttpStatus.NOT_FOUND, que + " no existe.", null, null);
    }

    public static NegocioException violaciones(String mensaje, List<Violacion> violaciones) {
        return new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje, null, violaciones);
    }
}
