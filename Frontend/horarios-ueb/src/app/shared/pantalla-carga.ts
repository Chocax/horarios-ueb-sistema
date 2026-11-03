// Pantalla de carga institucional: logo UEB, barra de progreso y mensajes que avanzan mientras el servidor responde.
// Su apariencia coincide con el bloque estático de index.html para que no haya salto al arrancar Angular.
import { ChangeDetectionStrategy, Component, computed, input, OnDestroy, OnInit, signal } from '@angular/core';

const PASOS = [
  'Conectando con el servidor…',
  'Despertando el servicio de horarios…',
  'Cargando el plan de estudios y las aulas…',
  'Ya casi: preparando el inicio de sesión…',
];

@Component({
  selector: 'app-pantalla-carga',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pc" role="status" aria-live="polite" [attr.aria-label]="mensaje()">
      <div class="pc-centro">
        <img class="pc-logo" src="ueb-logo.png" alt="Universidad El Bosque" width="150" height="129" />
        <div class="pc-titulo">Horarios UEB</div>
        <div class="pc-sub">Facultad de Ingeniería · Programa de Ingeniería de Sistemas</div>
        <div class="pc-barra" aria-hidden="true"><span></span></div>
        <div class="pc-msg">{{ mensaje() }}</div>
        @if (lento()) {
          <div class="pc-nota">La primera visita del día puede tardar hasta un minuto mientras el servidor se activa.</div>
        }
      </div>
      <div class="pc-lema">Por una cultura de la vida, su calidad y su sentido</div>
    </div>
  `,
  styles: [
    `
      :host { position: fixed; inset: 0; z-index: 200; }
      .pc { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
        background: var(--color-paper-warmth); animation: pc-entra 300ms ease; }
      .pc-centro { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 0 24px; }
      .pc-logo { width: 150px; height: auto; animation: pc-respira 2.4s ease-in-out infinite; }
      .pc-titulo { margin-top: 22px; font-size: 26px; font-weight: 600; letter-spacing: -0.6px; color: var(--ink-95); }
      .pc-sub { margin-top: 4px; font-size: 13.5px; color: var(--ink-60); }
      .pc-barra { position: relative; width: 220px; height: 4px; margin-top: 26px; border-radius: 99px; background: var(--color-sky-tint); overflow: hidden; }
      .pc-barra span { position: absolute; inset: 0; width: 40%; border-radius: 99px; background: var(--color-notion-blue); animation: pc-avanza 1.2s ease-in-out infinite; }
      .pc-msg { margin-top: 14px; min-height: 20px; font-size: 14px; font-weight: 500; color: var(--ink-90); }
      .pc-nota { margin-top: 8px; max-width: 340px; font-size: 12.5px; line-height: 1.45; color: var(--ink-60); }
      .pc-lema { position: absolute; bottom: 28px; font-family: var(--font-serif); font-style: italic; font-size: 14px; color: var(--ink-54); padding: 0 16px; text-align: center; }
      @keyframes pc-avanza { from { transform: translateX(-110%); } to { transform: translateX(260%); } }
      @keyframes pc-respira { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(0.97); opacity: 0.85; } }
      @keyframes pc-entra { from { opacity: 0; } }
      @media (prefers-reduced-motion: reduce) { .pc-logo { animation: none; } .pc-barra span { animation-duration: 3s; } }
      @media (max-width: 480px) { .pc-logo { width: 116px; } .pc-titulo { font-size: 22px; } }
    `,
  ],
})
export class PantallaCarga implements OnInit, OnDestroy {
  /** Mensaje fijo; si se omite, los mensajes avanzan solos según el tiempo de espera. */
  readonly texto = input<string>('');
  private readonly paso = signal(0);
  protected readonly mensaje = computed(() => this.texto() || PASOS[Math.min(this.paso(), PASOS.length - 1)]);
  protected readonly lento = computed(() => this.paso() >= 1);
  private reloj?: ReturnType<typeof setInterval>;

  ngOnInit() {
    this.reloj = setInterval(() => this.paso.update((p) => p + 1), 6000);
  }

  ngOnDestroy() {
    clearInterval(this.reloj);
  }
}
