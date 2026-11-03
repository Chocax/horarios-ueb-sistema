import { effect, Injectable, signal } from '@angular/core';

export type Tema = 'claro' | 'oscuro';

const CLAVE = 'ueb-horarios-tema';

/**
 * Tema visual (claro u oscuro). La elección se guarda en localStorage, así que solo vale en este navegador:
 * en otro navegador o computador la aplicación arranca en claro. public/tema.js la aplica antes de que cargue Angular.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly actual = signal<Tema>(this.leer());

  constructor() {
    effect(() => {
      const raiz = document.documentElement;
      if (this.actual() === 'oscuro') raiz.setAttribute('data-theme', 'dark');
      else raiz.removeAttribute('data-theme');
    });
  }

  alternar() {
    const t: Tema = this.actual() === 'oscuro' ? 'claro' : 'oscuro';
    this.actual.set(t);
    try {
      localStorage.setItem(CLAVE, t);
    } catch {}
  }

  private leer(): Tema {
    try {
      return localStorage.getItem(CLAVE) === 'oscuro' ? 'oscuro' : 'claro';
    } catch {
      return 'claro';
    }
  }
}
