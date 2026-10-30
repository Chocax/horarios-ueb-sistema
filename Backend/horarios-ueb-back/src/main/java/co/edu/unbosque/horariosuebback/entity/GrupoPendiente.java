package co.edu.unbosque.horariosuebback.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Grupo que el generador no pudo ubicar en una versión, con la regla y la causa (RF-08). */
@Entity
@Table(name = "grupo_pendiente")
@IdClass(GrupoPendiente.Llave.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoPendiente {

    @Id
    private String versionId;

    @Id
    private String grupoId;

    private String regla;
    private String causa;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Llave implements Serializable {
        private String versionId;
        private String grupoId;
    }
}
