package co.edu.unbosque.horariosuebback.exception;

import co.edu.unbosque.horariosuebback.dto.Dtos.Respuesta;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import co.edu.unbosque.horariosuebback.security.UsuarioActual;
import co.edu.unbosque.horariosuebback.service.BitacoraService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Convierte las excepciones en respuestas JSON en español, sin exponer detalles internos ni trazas. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private final BitacoraService bitacora;
    private final SesionActual sesion;

    public ManejadorErrores(BitacoraService bitacora, SesionActual sesion) {
        this.bitacora = bitacora;
        this.sesion = sesion;
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Respuesta<Void>> negocio(NegocioException e) {
        return ResponseEntity.status(e.getEstado()).body(new Respuesta<>(false, e.getMessage(), e.getErrores(), e.getViolaciones(), null));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Respuesta<Void>> sinPermiso(RuntimeException e, HttpServletRequest req) {
        UsuarioActual u = sesion.usuario();
        bitacora.registrarAparte(u == null ? "Anónimo" : u.nombre(), u == null ? "SISTEMA" : u.rol().name(), "Acción rechazada por permisos",
                "Rol " + (u == null ? "sin sesión" : u.rol()) + " intentó " + req.getMethod() + " " + req.getRequestURI());
        return error(HttpStatus.FORBIDDEN, "Operación rechazada por el servidor: su rol no tiene permiso para esta acción.", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Respuesta<Void>> invalido(MethodArgumentNotValidException e) {
        List<String> errores = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return error(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.", errores);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Respuesta<Void>> malFormado(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Respuesta<Void>> integridad(DataIntegrityViolationException e) {
        log.warn("Restricción de la base de datos: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "La base de datos rechazó el cambio porque crea un dato repetido o un cruce. Recargue e intente de nuevo.", null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Respuesta<Void>> concurrencia(OptimisticLockingFailureException e) {
        return error(HttpStatus.CONFLICT, "Otra persona modificó estos datos al mismo tiempo. Recargue e intente de nuevo.", null);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Respuesta<Void>> noExiste(Exception e) {
        return error(HttpStatus.NOT_FOUND, "Recurso no encontrado.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Respuesta<Void>> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error en el servidor. Intente de nuevo en unos segundos.", null);
    }

    private static ResponseEntity<Respuesta<Void>> error(HttpStatus estado, String mensaje, List<String> errores) {
        return ResponseEntity.status(estado).body(new Respuesta<>(false, mensaje, errores, null, null));
    }
}
