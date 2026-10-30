package co.edu.unbosque.horariosuebback.domain.reglas;

/** Incumplimiento de una restricción dura: código de la regla (R1…R9) y mensaje en español. */
public record Violacion(String regla, String mensaje) {
}
