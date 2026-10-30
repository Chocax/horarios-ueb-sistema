package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.ModoAtencion;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Horario de atención del docente por materia: 1 o 2 h semanales fijas o «acordar con el docente» (RF-23). */
@Entity
@Table(name = "horario_atencion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtencion {

    @Id
    private String id;
    private String periodoId;
    private String docenteId;
    private String cursoId;

    @Enumerated(EnumType.STRING)
    private ModoAtencion modo;

    private Integer dia;
    private Integer inicio;
    private Integer fin;
    private String lugar;
}
