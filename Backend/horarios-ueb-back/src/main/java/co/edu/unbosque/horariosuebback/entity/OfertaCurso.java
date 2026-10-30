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

/** Relación SE OFRECE EN (curso – periodo) con su demanda estimada (RF-18). */
@Entity
@Table(name = "oferta_curso")
@IdClass(OfertaCurso.Llave.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfertaCurso {

    @Id
    private String periodoId;

    @Id
    private String cursoId;

    private int demandaEstimada;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Llave implements Serializable {
        private String periodoId;
        private String cursoId;
    }
}
