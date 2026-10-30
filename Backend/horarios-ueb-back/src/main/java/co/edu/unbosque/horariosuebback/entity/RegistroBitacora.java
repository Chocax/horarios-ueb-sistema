package co.edu.unbosque.horariosuebback.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Registro de auditoría: quién hizo qué y cuándo (RF-15). Solo se inserta. */
@Entity
@Table(name = "bitacora")
@Getter
@Setter
@NoArgsConstructor
public class RegistroBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant fecha;
    private String usuario;
    private String rol;
    private String accion;
    private String detalle;
}
