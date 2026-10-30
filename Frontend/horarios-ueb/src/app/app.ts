import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { StoreService } from './core/services/store.service';
import { ToastHost } from './shared/toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastHost],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (store.cargando()) { <div class="carga" role="progressbar" aria-label="Cargando datos del servidor"></div> }<router-outlet /><app-toast-host />`,
  styles: [
    `.carga { position: fixed; top: 0; left: 0; right: 0; height: 3px; z-index: 100; overflow: hidden; background: var(--color-sky-tint); }
     .carga::after { content: ''; position: absolute; inset: 0; width: 40%; background: var(--color-notion-blue); animation: carga 1.1s ease-in-out infinite; }
     @keyframes carga { from { transform: translateX(-100%); } to { transform: translateX(250%); } }`,
  ],
})
export class App {
  protected readonly store = inject(StoreService);
}
