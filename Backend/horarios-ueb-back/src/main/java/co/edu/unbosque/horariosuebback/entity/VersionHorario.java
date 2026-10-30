package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Versión del horario de un periodo (RF-12). Solo una en BORRADOR y una PUBLICADA por periodo. */
@Entity
@Table(name = "version_horario")
@Getter
@Setter
@NoArgsConstructor
public class VersionHorario {

    @Id
    private String id;
    private String periodoId;
    private int numero;

    @Enumerated(EnumType.STRING)
    private EstadoVersion estado;

    private String origen;
    private Instant fechaCreacion;
    private Instant fechaPublicacion;
    private String autor;
}
