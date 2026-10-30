package co.edu.unbosque.horariosuebback.domain.reglas;

/** Asignación que se valida: una sesión existente que se mueve (sesionId) o una nueva (sesionId = null). */
public record Asignacion(String sesionId, String grupoId, int dia, int inicio, String docenteId, String aulaId) {
}
