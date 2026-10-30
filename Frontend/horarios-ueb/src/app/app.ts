import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { StoreService } from './core/services/store.service';
import { ToastHost } from './shared/toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastHost],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (store.ocupado() || store.cargando()) { <div class="carga" role="progressbar" aria-label="Procesando"></div> }
    @if (aviso()) {
      <div class="procesando" role="status" aria-live="polite">
        <span class="spinner" aria-hidden="true"></span>
        <div>
          <strong>Procesando su solicitud…</strong>
          @if (lento()) { <span>El servidor se está activando; puede tardar hasta un minuto. No cierre la página.</span> }
          @else { <span>Su solicitud fue enviada y se está procesando.</span> }
        </div>
      </div>
    }
    <router-outlet /><app-toast-host />
  `,
  styles: [
    `.carga { position: fixed; top: 0; left: 0; right: 0; height: 3px; z-index: 100; overflow: hidden; background: var(--color-sky-tint); }
     .carga::after { content: ''; position: absolute; inset: 0; width: 40%; background: var(--color-notion-blue); animation: carga 1.1s ease-in-out infinite; }
     @keyframes carga { from { transform: translateX(-100%); } to { transform: translateX(250%); } }
     .procesando { position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 90; display: flex; align-items: center; gap: 12px;
       max-width: min(440px, calc(100vw - 32px)); padding: 12px 16px; border-radius: 12px; background: #fff; border: 1px solid var(--hairline);
       box-shadow: var(--shadow-mockup, 0 8px 24px rgba(0,0,0,.12)); animation: entra 200ms ease; }
     .procesando div { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
     .procesando span:not(.spinner) { color: var(--ink-60); }
     .spinner { flex: none; width: 20px; height: 20px; border-radius: 50%; border: 2.5px solid var(--color-sky-tint); border-top-color: var(--color-notion-blue);
       animation: gira 0.8s linear infinite; }
     @keyframes gira { to { transform: rotate(360deg); } }
     @keyframes entra { from { opacity: 0; transform: translate(-50%, 8px); } }
     @media (prefers-reduced-motion: reduce) { .carga::after, .spinner { animation-duration: 3s; } }`,
  ],
})
export class App {
  protected readonly store = inject(StoreService);
  /** El aviso aparece solo si la espera pasa de medio segundo (las respuestas rápidas no parpadean). */
  protected readonly aviso = signal(false);
  protected readonly lento = signal(false);

  constructor() {
    let t1: ReturnType<typeof setTimeout> | undefined;
    let t2: ReturnType<typeof setTimeout> | undefined;
    effect(() => {
      const ocupado = this.store.ocupado() || this.store.cargando();
      clearTimeout(t1);
      clearTimeout(t2);
      if (ocupado) {
        t1 = setTimeout(() => this.aviso.set(true), 500);
        t2 = setTimeout(() => this.lento.set(true), 8000);
      } else {
        this.aviso.set(false);
        this.lento.set(false);
      }
    });
  }
}
