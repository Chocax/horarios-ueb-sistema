package co.edu.unbosque.horariosuebback.domain.reglas;

/**
 * Patrón Specification: cada restricción dura es una clase que implementa esta interfaz.
 * Agregar una regla nueva = agregar una clase a la lista de {@link ValidadorReglas} (RNF-03).
 */
public interface ReglaDura {

    String codigo();

    String nombre();

    /** Devuelve la violación o null si la asignación cumple la regla. */
    Violacion evaluar(ContextoValidacion ctx);
}
