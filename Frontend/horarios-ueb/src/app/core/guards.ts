import { ErrorHandler, inject, Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RUTA_INICIAL } from '../layout/nav';
import { Rol } from './models';
import { SessionService } from './services/session.service';
import { StoreService } from './services/store.service';
import { ToastService } from './services/toast.service';

/** Exige sesión y, si la página se recargó, vuelve a traer el estado del servidor antes de mostrar la pantalla. */
export const autenticado: CanActivateFn = async (_route, state) => {
  const s = inject(SessionService);
  const store = inject(StoreService);
  const router = inject(Router);
  // se recuerda la pantalla pedida para volver a ella después de iniciar sesión (links compartidos)
  const destino = state.url && state.url !== '/' ? `?r=${encodeURIComponent(state.url)}` : '';
  if (!s.token()) return router.parseUrl(`/login${destino}`);
  if (!store.listo()) {
    const r = await store.cargar('');
    if (!r.ok) {
      inject(ToastService).error('No se pudieron cargar los datos', [r.mensaje ?? '']);
      store.cerrarSesion();
      return router.parseUrl(`/login${destino}`);
    }
  }
  return true;
};

/** Guarda por rol: oculta la pantalla en el menú y bloquea la ruta. Los permisos reales los aplica el servidor. */
export const conRol = (...roles: Rol[]): CanActivateFn => () => {
  const s = inject(SessionService);
  const t = inject(ToastService);
  const actual = s.actual();
  if (!actual) return inject(Router).parseUrl('/login');
  if (roles.includes(actual.rol)) return true;
  t.error('Acceso denegado', ['Su rol no tiene permiso para esta pantalla.']);
  return inject(Router).parseUrl(RUTA_INICIAL[actual.rol]);
};

export const redirigirPorRol: CanActivateFn = () => {
  const s = inject(SessionService);
  const actual = s.actual();
  return inject(Router).parseUrl(actual ? RUTA_INICIAL[actual.rol] : '/login');
};

@Injectable()
export class ManejadorErrores implements ErrorHandler {
  private readonly toast = inject(ToastService);
  handleError(error: unknown): void {
    console.error(error);
    this.toast.error('Ocurrió un error inesperado', ['Recargue la página e intente de nuevo.']);
  }
}
