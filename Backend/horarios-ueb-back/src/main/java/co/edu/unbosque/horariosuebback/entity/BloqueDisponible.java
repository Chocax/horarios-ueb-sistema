package co.edu.unbosque.horariosuebback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bloque de disponibilidad semanal del docente. dia: 0 = lunes … 5 = sábado. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloqueDisponible {

    @Column(nullable = false)
    private int dia;

    @Column(nullable = false)
    private int inicio;

    @Column(nullable = false)
    private int fin;
}
