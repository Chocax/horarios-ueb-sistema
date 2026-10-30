// Administración: borrar versiones del horario de prueba, como si nunca hubieran existido
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { VersionHorario } from '../../core/models';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { fechaCorta } from '../../core/util';
import { Icon } from '../../shared/icon';
import { Modal } from '../../shared/modal';

type Confirmacion = { tipo: 'una'; version: VersionHorario } | { tipo: 'todas' };

@Component({
  selector: 'app-propuestas',
  imports: [FormsModule, Icon, Modal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <div class="eyebrow">Administración</div>
          <h1>Propuestas de horario</h1>
          <p class="serif sub">Borre las versiones de prueba del periodo {{ store.periodo().codigo }} con sus sesiones, pendientes y observaciones del comité. Desaparecen de todas las pantallas y la numeración continúa desde la versión más alta que quede. La bitácora conserva el registro de la eliminación.</p>
        </div>
      </div>

      <div class="card acciones">
        <div>
          <strong>{{ noPublicadas().length }} propuesta(s) sin publicar</strong>
          <span class="caption">{{ versiones().length }} versión(es) en total en {{ store.periodo().codigo }}. Cambie de periodo en la barra superior.</span>
        </div>
        <label class="check"><input type="checkbox" [(ngModel)]="incluirPublicada" /> Incluir también la versión publicada</label>
        <button class="btn btn-danger" (click)="confirmar.set({ tipo: 'todas' })" [disabled]="store.ocupado() || !(incluirPublicada ? versiones().length : noPublicadas().length)">
          <app-icon name="alert" [size]="16" /> Borrar {{ incluirPublicada ? 'todas las versiones' : 'todas las propuestas sin publicar' }}
        </button>
      </div>

      <div class="tabla-wrap">
        <table class="tabla">
          <thead><tr><th>Versión</th><th>Estado</th><th>Origen</th><th>Creada</th><th class="num">Sesiones</th><th class="num">Pendientes</th><th></th></tr></thead>
          <tbody>
            @for (v of versiones(); track v.id) {
              <tr>
                <td><strong>v{{ v.numero }}</strong></td>
                <td><span class="pill" [class.green]="v.estado === 'PUBLICADA'" [class.yellow]="v.estado === 'BORRADOR'">{{ estado(v) }}</span></td>
                <td class="caption">{{ v.origen }}</td>
                <td class="caption">{{ v.autor }} · {{ fecha(v.fechaCreacion) }}</td>
                <td class="num">{{ v.sesiones.length }}</td>
                <td class="num">{{ v.pendientes.length }}</td>
                <td class="num"><button class="btn btn-sm" (click)="confirmar.set({ tipo: 'una', version: v })" [disabled]="store.ocupado()">Borrar</button></td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="muted">No hay versiones del horario en {{ store.periodo().codigo }}.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    @if (confirmar(); as c) {
      <app-modal [titulo]="c.tipo === 'una' ? 'Borrar la versión ' + c.version.numero : 'Borrar versiones de ' + store.periodo().codigo"
                 subtitulo="Esta acción no se puede deshacer." (cerrar)="confirmar.set(null)">
        @if (c.tipo === 'una' && c.version.estado === 'PUBLICADA') {
          <div class="aviso warn"><app-icon name="alert" [size]="18" /><div>Es el horario <strong>publicado</strong>: docentes y comité dejarán de verlo hasta que se publique otra versión.</div></div>
        } @else if (c.tipo === 'todas' && incluirPublicada) {
          <div class="aviso warn"><app-icon name="alert" [size]="18" /><div>Se borrará también el horario <strong>publicado</strong> del periodo.</div></div>
        } @else {
          <p class="muted">Se borrarán sus sesiones, grupos pendientes y observaciones del comité.</p>
        }
        <div class="modal-foot">
          <button class="btn btn-ghost" (click)="confirmar.set(null)">Cancelar</button>
          <button class="btn btn-danger" (click)="borrar(c)" [disabled]="store.ocupado()">Borrar definitivamente</button>
        </div>
      </app-modal>
    }
  `,
  styles: [
    `.acciones { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
     .acciones > div { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 220px; }`,
  ],
})
export class Propuestas {
  protected readonly store = inject(StoreService);
  private readonly toast = inject(ToastService);
  protected readonly fecha = fechaCorta;
  protected readonly versiones = this.store.versionesPeriodo;
  protected readonly noPublicadas = computed(() => this.versiones().filter((v) => v.estado !== 'PUBLICADA'));
  protected readonly confirmar = signal<Confirmacion | null>(null);
  incluirPublicada = false;

  estado(v: VersionHorario) {
    return v.estado === 'BORRADOR' ? 'Borrador' : v.estado === 'PUBLICADA' ? 'Publicada' : 'Archivada';
  }

  async borrar(c: Confirmacion) {
    const r = c.tipo === 'una' ? await this.store.eliminarVersion(c.version.id) : await this.store.eliminarVersionesPeriodo(this.incluirPublicada);
    this.confirmar.set(null);
    if (r.ok) this.toast.ok(c.tipo === 'una' ? `Versión ${c.version.numero} borrada` : (r.mensaje ?? 'Versiones borradas'));
    else this.toast.error(r.mensaje ?? 'No se pudo borrar', r.errores);
  }
}
