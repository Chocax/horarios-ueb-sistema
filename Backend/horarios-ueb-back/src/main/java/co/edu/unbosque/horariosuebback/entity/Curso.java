package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;
import co.edu.unbosque.horariosuebback.entity.enums.TipoCurso;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** Curso del plan de estudios (RF-05). nivel = semestre del plan. */
@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    private String id;
    private String codigo;
    private String nombre;
    private int nivel;
    private int creditos;
    private int sesionesSemanales;

    @Enumerated(EnumType.STRING)
    private TipoCurso tipo;

    @ElementCollection
    @CollectionTable(name = "curso_caracteristica", joinColumns = @JoinColumn(name = "curso_id"))
    @Column(name = "caracteristica")
    @Enumerated(EnumType.STRING)
    private Set<Caracteristica> caracteristicasRequeridas = new HashSet<>();
}
