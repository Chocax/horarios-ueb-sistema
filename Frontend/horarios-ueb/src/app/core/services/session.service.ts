import { Injectable, signal } from '@angular/core';
import { Rol } from '../models';

export interface SesionUsuario {
  usuarioId: string;
  nombre: string;
  correo: string;
  rol: Rol;
  docenteId?: string | null;
  /** Token de acceso (JWT) que emite el backend. */
  token: string;
  /** Fecha de vencimiento del token (ms desde 1970). */
  expira: number;
}

const CLAVE = 'ueb-horarios-sesion';

/**
 * Sesión del usuario autenticado. Se guarda en sessionStorage: se borra al cerrar la pestaña.
 * El rol solo sirve para el menú; los permisos los aplica el servidor en cada petición.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly actual = signal<SesionUsuario | null>(this.leer());

  iniciar(s: SesionUsuario) {
    this.actual.set(s);
    try {
      sessionStorage.setItem(CLAVE, JSON.stringify(s));
    } catch {}
  }

  cerrar() {
    this.actual.set(null);
    try {
      sessionStorage.removeItem(CLAVE);
    } catch {}
  }

  token() {
    const s = this.actual();
    if (s && s.expira <= Date.now()) {
      this.cerrar();
      return null;
    }
    return s?.token ?? null;
  }

  tieneRol(...roles: Rol[]) {
    const s = this.actual();
    return !!s && roles.includes(s.rol);
  }

  private leer(): SesionUsuario | null {
    try {
      const t = sessionStorage.getItem(CLAVE);
      const s = t ? (JSON.parse(t) as SesionUsuario) : null;
      return s && s.token && s.expira > Date.now() ? s : null;
    } catch {
      return null;
    }
  }
}
