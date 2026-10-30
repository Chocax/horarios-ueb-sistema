package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.Concepto;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Concepto u observación del comité sobre una versión (RF-20). */
@Entity
@Table(name = "observacion")
@Getter
@Setter
@NoArgsConstructor
public class Observacion {

    @Id
    private String id;
    private String versionId;
    private String usuarioId;
    private String autor;

    @Enumerated(EnumType.STRING)
    private Concepto concepto;

    private String texto;
    private Instant fecha;
}
