// Cliente de la API REST del backend (Spring Boot). Convierte toda respuesta en un Resultado y nunca lanza.
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Violacion } from '../models';
import { SessionService } from './session.service';

export interface Resultado<T = void> {
  ok: boolean;
  mensaje?: string;
  errores?: string[];
  violaciones?: Violacion[];
  valor?: T;
}

export const API_URL = environment.apiUrl;

/** Agrega el token Bearer solo a las peticiones dirigidas a la API propia. */
export const interceptorToken: HttpInterceptorFn = (req, next) => {
  const token = inject(SessionService).token();
  if (token && req.url.startsWith(API_URL)) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(SessionService);
  private readonly router = inject(Router);
  /** Peticiones en curso (para mostrar que el sistema está procesando). */
  readonly pendientes = signal(0);

  get<T>(ruta: string) {
    return this.pedir<T>('GET', ruta);
  }
  post<T>(ruta: string, cuerpo: unknown = {}) {
    return this.pedir<T>('POST', ruta, cuerpo);
  }
  put<T>(ruta: string, cuerpo: unknown) {
    return this.pedir<T>('PUT', ruta, cuerpo);
  }
  patch<T>(ruta: string, cuerpo: unknown) {
    return this.pedir<T>('PATCH', ruta, cuerpo);
  }
  delete<T>(ruta: string) {
    return this.pedir<T>('DELETE', ruta);
  }

  /** Despierta el servidor (en el plan gratuito se duerme tras 15 min sin uso). */
  async salud(): Promise<boolean> {
    try {
      await firstValueFrom(this.http.get(`${API_URL}/salud`).pipe(timeout(90_000)));
      return true;
    } catch {
      return false;
    }
  }

  private async pedir<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<Resultado<T>> {
    this.pendientes.update((n) => n + 1);
    try {
      const r = await firstValueFrom(this.http.request<unknown>(metodo, `${API_URL}${ruta}`, { body: cuerpo }).pipe(timeout(90_000)));
      // las operaciones responden {ok, mensaje, valor}; las consultas devuelven el dato directamente
      if (r && typeof r === 'object' && !Array.isArray(r) && 'ok' in r) return r as Resultado<T>;
      return { ok: true, valor: r as T };
    } catch (e) {
      const err = e as HttpErrorResponse;
      if (!(e instanceof HttpErrorResponse) || err.status === 0) {
        return { ok: false, mensaje: 'No hay conexión con el servidor. Verifique su internet e intente de nuevo en unos segundos.' };
      }
      const cuerpoError = (err.error ?? {}) as Partial<Resultado<T>>;
      if (err.status === 401 && this.sesion.actual()) {
        this.sesion.cerrar();
        this.router.navigateByUrl('/login?expirada=1');
      }
      return {
        ok: false,
        mensaje: cuerpoError.mensaje ?? (err.status === 403 ? 'No tiene permiso para esta operación.' : 'Ocurrió un error en el servidor.'),
        errores: cuerpoError.errores,
        violaciones: cuerpoError.violaciones,
      };
    } finally {
      this.pendientes.update((n) => Math.max(0, n - 1));
    }
  }
}
