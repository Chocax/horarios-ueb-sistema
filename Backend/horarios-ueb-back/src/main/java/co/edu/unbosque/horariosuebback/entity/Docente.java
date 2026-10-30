package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.Vinculacion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Docente con su vinculación, cursos habilitados y disponibilidad semanal (RF-03, RF-04). */
@Entity
@Table(name = "docente")
@Getter
@Setter
@NoArgsConstructor
public class Docente {

    @Id
    private String id;
    private String identificador;
    private String nombre;
    private String correoInstitucional;

    @Enumerated(EnumType.STRING)
    private Vinculacion tipoVinculacion;

    private String categoriaEscalafon;
    private int horasMaxDocencia;

    /** Ids de los cursos que puede orientar (R5). */
    @ElementCollection
    @CollectionTable(name = "docente_habilitacion", joinColumns = @JoinColumn(name = "docente_id"))
    @Column(name = "curso_id")
    private Set<String> habilitaciones = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "docente_disponibilidad", joinColumns = @JoinColumn(name = "docente_id"))
    @OrderBy("dia ASC, inicio ASC")
    private List<BloqueDisponible> disponibilidad = new ArrayList<>();
}
