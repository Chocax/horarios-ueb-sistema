package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Grupo (sección) de un curso en un periodo (RF-07). */
@Entity
@Table(name = "grupo")
@Getter
@Setter
@NoArgsConstructor
public class Grupo {

    @Id
    private String id;
    private String periodoId;
    private String cursoId;
    private String codigo;

    @Enumerated(EnumType.STRING)
    private Jornada jornada;

    private int cupoPlaneado;
    private Integer inscritos;

    @Enumerated(EnumType.STRING)
    private EstadoGrupo estado;

    /** Docente que el generador intenta primero (asignación acordada con el docente). */
    @Column(name = "docente_preferido_id")
    private String docentePreferido;
}
