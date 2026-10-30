// Inicio de sesión con el correo institucional
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { SessionService } from '../../core/services/session.service';
import { StoreService } from '../../core/services/store.service';
import { RUTA_INICIAL } from '../../layout/nav';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="wrap">
      <section class="hero">
        <div class="marca-ueb">
          <img src="ueb-logo.png" alt="Universidad El Bosque" width="132" height="114" />
          <div class="sep"></div>
          <div><strong>Facultad de Ingeniería</strong><span>Programa de Ingeniería de Sistemas</span></div>
        </div>
        <h1>Horarios de clase <span class="highlight">claros y a tiempo</span> para toda la comunidad.</h1>
        <p class="serif">
          El portal de programación académica del Programa de Ingeniería de Sistemas: la coordinación arma y publica el horario de cada
          periodo, el comité lo revisa y cada docente consulta sus clases y registra su disponibilidad.
        </p>
        <div class="tarjetas">
          <div class="accent marigold"><strong>Docentes</strong><span>consulten su horario publicado y registren su disponibilidad y horario de atención</span></div>
          <div class="accent tint"><strong>Coordinación</strong><span>programe, ajuste y publique el horario del periodo sin choques</span></div>
          <div class="accent midnight"><strong>Comité</strong><span>revise cada versión y deje su concepto antes de la publicación</span></div>
        </div>
        <p class="caption lema">Universidad El Bosque · Por una cultura de la vida, su calidad y su sentido</p>
      </section>

      <section class="card form">
        <h2>Iniciar sesión</h2>
        <p class="muted" style="margin:4px 0 18px">Ingrese con su correo institucional.</p>
        @if (aviso()) {
          <div class="aviso" style="margin-bottom:14px" role="status">{{ aviso() }}</div>
        }
        <form (ngSubmit)="entrar()" class="stack">
          <div class="field">
            <label for="correo">Correo institucional</label>
            <input id="correo" type="email" name="correo" [(ngModel)]="correo" autocomplete="username" maxlength="120" required />
          </div>
          <div class="field">
            <label for="clave">Contraseña</label>
            <input id="clave" type="password" name="clave" [(ngModel)]="clave" autocomplete="current-password" maxlength="128" required />
          </div>
          @if (error()) {
            <div class="errores" role="alert">{{ error() }}</div>
          }
          <button class="btn btn-primary" type="submit" style="margin-top:4px" [disabled]="enviando()">
            {{ enviando() ? 'Ingresando…' : 'Entrar' }}
          </button>
        </form>
        <p class="caption ayuda">¿Olvidó su contraseña o no tiene usuario? Escriba a la coordinación del Programa de Ingeniería de Sistemas.</p>
      </section>
    </div>
  `,
  styles: [
    `
      .wrap { min-height: 100vh; display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 48px; align-items: start; max-width: 1280px; margin: 0 auto; padding: 56px 40px 48px; }
      .hero { position: sticky; top: 56px; padding-top: 8px; }
      @media (max-width: 1000px) {
        .wrap { grid-template-columns: 1fr; gap: 28px; padding: 28px 20px 40px; }
        .hero { position: static; }
      }
      .marca-ueb { display: flex; align-items: center; gap: 18px; margin-bottom: 28px; }
      .marca-ueb img { width: 104px; height: auto; }
      .marca-ueb .sep { width: 1px; align-self: stretch; background: var(--hairline); }
      .marca-ueb strong { display: block; font-size: 16px; }
      .marca-ueb span { font-size: 13px; color: var(--ink-60); }
      .hero h1 { font-size: 54px; line-height: 1.12; letter-spacing: -1.89px; font-weight: 600; max-width: 640px; }
      .hero h1 .highlight { position: relative; z-index: -1; }
      .hero .serif { margin-top: 18px; max-width: 560px; }
      .tarjetas { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 32px; max-width: 640px; }
      .tarjetas .accent { padding: 16px; display: flex; flex-direction: column; gap: 4px; }
      .tarjetas strong { font-size: 20px; letter-spacing: -0.4px; }
      .tarjetas span { font-size: 13px; line-height: 1.35; }
      .lema { margin-top: 22px; }
      .form { padding: 28px; }
      .ayuda { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--hairline); }
      @media (max-width: 640px) {
        .marca-ueb { gap: 12px; margin-bottom: 18px; }
        .marca-ueb img { width: 72px; }
        .hero h1 { font-size: 34px; letter-spacing: -1px; }
        .hero .serif { font-size: 16px; margin-top: 12px; }
        .tarjetas { grid-template-columns: 1fr; gap: 8px; margin-top: 20px; }
        .tarjetas .accent { flex-direction: row; align-items: baseline; gap: 10px; padding: 12px 14px; }
        .tarjetas strong { font-size: 17px; flex: none; }
        .form { padding: 20px; }
      }
    `,
  ],
})
export class Login implements OnInit {
  private readonly store = inject(StoreService);
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  correo = '';
  clave = '';
  readonly error = signal('');
  readonly aviso = signal('');
  readonly enviando = signal(false);

  ngOnInit() {
    if (this.route.snapshot.queryParamMap.get('expirada')) this.aviso.set('Su sesión expiró. Inicie sesión de nuevo.');
    // el servidor gratuito se duerme tras un rato sin uso: se despierta mientras la persona escribe
    const lento = setTimeout(() => !this.aviso() && this.aviso.set('Conectando con el servidor… la primera vez del día puede tardar hasta un minuto.'), 2500);
    this.api.salud().then((ok) => {
      clearTimeout(lento);
      if (this.aviso().startsWith('Conectando')) this.aviso.set(ok ? '' : 'El servidor no responde. Intente de nuevo en unos minutos.');
    });
  }

  async entrar() {
    if (this.enviando()) return;
    if (!this.correo.trim() || !this.clave) {
      this.error.set('Escriba su correo y su contraseña.');
      return;
    }
    this.enviando.set(true);
    const r = await this.store.iniciarSesion(this.correo, this.clave);
    this.enviando.set(false);
    if (!r.ok) {
      this.error.set(r.mensaje ?? 'No se pudo iniciar sesión.');
      return;
    }
    this.error.set('');
    this.clave = '';
    const destino = this.route.snapshot.queryParamMap.get('r');
    this.router.navigateByUrl(destino && destino.startsWith('/') && !destino.startsWith('//') && !destino.startsWith('/login') ? destino : RUTA_INICIAL[this.sesion.actual()!.rol]);
  }
}
