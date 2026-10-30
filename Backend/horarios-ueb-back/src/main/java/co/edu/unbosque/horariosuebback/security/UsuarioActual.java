package co.edu.unbosque.horariosuebback.security;

import co.edu.unbosque.horariosuebback.entity.enums.Rol;

/** Usuario autenticado de la petición en curso (leído de la base de datos en cada petición). */
public record UsuarioActual(String id, String nombre, Rol rol, String docenteId) {

    public boolean es(Rol... roles) {
        for (Rol r : roles) if (r == rol) return true;
        return false;
    }
}
