package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;
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

/** Aula con su capacidad y características (RF-06). */
@Entity
@Table(name = "aula")
@Getter
@Setter
@NoArgsConstructor
public class Aula {

    @Id
    private String id;
    private String codigo;
    private String nombre;
    private int capacidad;
    private String bloque;
    private Integer piso;

    @ElementCollection
    @CollectionTable(name = "aula_caracteristica", joinColumns = @JoinColumn(name = "aula_id"))
    @Column(name = "caracteristica")
    @Enumerated(EnumType.STRING)
    private Set<Caracteristica> caracteristicas = new HashSet<>();
}
