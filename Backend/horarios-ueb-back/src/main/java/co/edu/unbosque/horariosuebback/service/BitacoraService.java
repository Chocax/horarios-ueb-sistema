package co.edu.unbosque.horariosuebback.service;

import co.edu.unbosque.horariosuebback.dto.Dtos.BitacoraDto;
import co.edu.unbosque.horariosuebback.entity.RegistroBitacora;
import co.edu.unbosque.horariosuebback.repository.BitacoraRepository;
import co.edu.unbosque.horariosuebback.security.SesionActual;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Bitácora de auditoría (RF-15): toda creación, modificación y publicación con usuario y fecha-hora. */
@Service
public class BitacoraService {

    private final BitacoraRepository repo;
    private final SesionActual sesion;

    public BitacoraService(BitacoraRepository repo, SesionActual sesion) {
        this.repo = repo;
        this.sesion = sesion;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(String accion, String detalle) {
        registrar(sesion.nombre(), sesion.rol(), accion, detalle);
    }

    /** Para eventos que deben quedar aunque la operación principal falle (p. ej. accesos rechazados). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAparte(String usuario, String rol, String accion, String detalle) {
        registrar(usuario, rol, accion, detalle);
    }

    private void registrar(String usuario, String rol, String accion, String detalle) {
        RegistroBitacora r = new RegistroBitacora();
        r.setFecha(Instant.now());
        r.setUsuario(recortar(usuario, 120));
        r.setRol(rol);
        r.setAccion(recortar(accion, 80));
        r.setDetalle(recortar(detalle == null ? "" : detalle, 1000));
        repo.save(r);
    }

    @Transactional(readOnly = true)
    public List<BitacoraDto> ultimos(int n) {
        return repo.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(Math.max(n, 1), 1000))).stream()
                .map(r -> new BitacoraDto(r.getId(), r.getFecha(), r.getUsuario(), r.getRol(), r.getAccion(), r.getDetalle()))
                .toList();
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
