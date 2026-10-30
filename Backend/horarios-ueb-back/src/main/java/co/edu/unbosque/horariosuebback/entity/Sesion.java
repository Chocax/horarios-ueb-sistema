package co.edu.unbosque.horariosuebback.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesión de clase de un grupo en una versión: día (0 = lunes … 5 = sábado), hora de inicio, docente y aula. */
@Entity
@Table(name = "sesion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sesion {

    @Id
    private String id;
    private String versionId;
    private String grupoId;
    private int dia;
    private int inicio;
    private String docenteId;
    private String aulaId;
}
