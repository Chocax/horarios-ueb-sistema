package co.edu.unbosque.horariosuebback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rango de horas enteras (24 h), p. ej. el bloque no asignable del almuerzo 13–14. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RangoHorario {

    @Column(nullable = false)
    private int inicio;

    @Column(nullable = false)
    private int fin;
}
