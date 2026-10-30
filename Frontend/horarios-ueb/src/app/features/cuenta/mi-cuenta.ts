// Mi cuenta: datos del usuario y cambio de contraseña (todos los roles)
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SessionService } from '../../core/services/session.service';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { nombreRol } from '../../core/util';

@Component({
  selector: 'app-mi-cuenta',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <div class="eyebrow">Cuenta</div>
          <h1>Mi cuenta</h1>
          <p class="serif sub">Sus datos de acceso al portal. Le recomendamos cambiar la contraseña inicial por una propia.</p>
        </div>
      </div>

      <div class="grid" style="grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); align-items:start">
        <div class="card">
          <div class="card-title"><h2>Datos</h2></div>
          <div class="dato"><span class="caption">Nombre</span><strong>{{ s()?.nombre }}</strong></div>
          <div class="dato"><span class="caption">Correo institucional</span><strong>{{ s()?.correo }}</strong></div>
          <div class="dato"><span class="caption">Rol</span><span class="rol" [class]="s()?.rol">{{ rol() }}</span></div>
          <p class="caption" style="margin-top:12px">Para cambiar su nombre, correo o rol escriba al administrador del sistema.</p>
        </div>

        <form class="card" (ngSubmit)="cambiar()">
          <div class="card-title"><h2>Cambiar contraseña</h2></div>
          <div class="stack">
            <div class="field">
              <label for="actual">Contraseña actual</label>
              <input id="actual" type="password" name="actual" [(ngModel)]="actual" autocomplete="current-password" maxlength="128" required />
            </div>
            <div class="field">
              <label for="nueva">Nueva contraseña</label>
              <input id="nueva" type="password" name="nueva" [(ngModel)]="nueva" autocomplete="new-password" maxlength="128" required />
              <span class="caption">Mínimo 8 caracteres, con al menos una letra y un número.</span>
            </div>
            <div class="field">
              <label for="confirmacion">Confirme la nueva contraseña</label>
              <input id="confirmacion" type="password" name="confirmacion" [(ngModel)]="confirmacion" autocomplete="new-password" maxlength="128" required />
            </div>
            @if (errores().length) {
              <div class="errores" role="alert"><ul>@for (e of errores(); track e) { <li>{{ e }}</li> }</ul></div>
            }
            <button class="btn btn-primary" type="submit" [disabled]="enviando()">{{ enviando() ? 'Guardando…' : 'Cambiar contraseña' }}</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [
    `.dato { display: flex; flex-direction: column; gap: 3px; padding: 8px 0; border-bottom: 1px solid var(--hairline); }
     .dato .rol { align-self: flex-start; }
     @media (max-width: 800px) { .grid { grid-template-columns: 1fr !important; } }`,
  ],
})
export class MiCuenta {
  private readonly store = inject(StoreService);
  private readonly sesion = inject(SessionService);
  private readonly toast = inject(ToastService);
  protected readonly s = this.sesion.actual;
  protected readonly rol = computed(() => (this.s() ? nombreRol(this.s()!.rol) : ''));
  protected readonly errores = signal<string[]>([]);
  protected readonly enviando = signal(false);
  actual = '';
  nueva = '';
  confirmacion = '';

  async cambiar() {
    const err: string[] = [];
    if (!this.actual) err.push('Escriba su contraseña actual.');
    if (this.nueva.length < 8 || !/[A-Za-zÁÉÍÓÚáéíóúÑñ]/.test(this.nueva) || !/\d/.test(this.nueva)) err.push('La nueva contraseña debe tener al menos 8 caracteres, una letra y un número.');
    if (this.nueva !== this.confirmacion) err.push('La confirmación no coincide con la nueva contraseña.');
    this.errores.set(err);
    if (err.length) return;
    this.enviando.set(true);
    const r = await this.store.cambiarClave(this.actual, this.nueva);
    this.enviando.set(false);
    if (!r.ok) {
      this.errores.set([r.mensaje ?? 'No se pudo cambiar la contraseña.', ...(r.errores ?? [])]);
      return;
    }
    this.actual = this.nueva = this.confirmacion = '';
    this.toast.ok('Contraseña actualizada', ['Úsela la próxima vez que inicie sesión.']);
  }
}
