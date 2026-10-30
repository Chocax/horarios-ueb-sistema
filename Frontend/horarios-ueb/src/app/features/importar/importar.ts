// CU-07 Importar datos del periodo · RF-17
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { PLANTILLAS, plantillaCsv, procesarImportacion, ResultadoImportacion, TipoImportacion } from '../../core/import/csv';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { descargarArchivo } from '../../core/util';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-importar',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <div class="eyebrow">Planeación</div>
          <h1>Importar datos</h1>
          <p class="serif sub">Cargue docentes, disponibilidades, cursos o aulas desde un CSV con la plantilla. Si alguna fila tiene errores no se carga ningún registro y se listan todas las filas con su motivo.</p>
        </div>
      </div>

      <div class="tabs" style="margin-bottom:18px">
        @for (t of tipos; track t.valor) {
          <button [class.on]="tipo() === t.valor" (click)="cambiarTipo(t.valor)">{{ t.nombre }}</button>
        }
      </div>

      <div class="grid" style="grid-template-columns: 1fr 1fr; align-items:start">
        <div class="card">
          <div class="card-title"><h2>1. Prepare el archivo</h2></div>
          <p class="muted">Columnas de la plantilla (separador <code>;</code> o <code>,</code>, se detecta solo):</p>
          <div class="cols">@for (c of plantilla().columnas; track c) { <code>{{ c }}</code> }</div>
          <p class="caption" style="margin:10px 0 14px">{{ plantilla().ayuda }}</p>
          <button class="btn btn-sm" (click)="descargarPlantilla()"><app-icon name="download" [size]="14" /> Descargar plantilla</button>
        </div>

        <div class="card">
          <div class="card-title"><h2>2. Suba y valide</h2></div>
          <label class="drop" [class.over]="sobre()" (dragover)="$event.preventDefault(); sobre.set(true)" (dragleave)="sobre.set(false)" (drop)="soltar($event)">
            <app-icon name="upload" [size]="26" />
            <strong>Arrastre el CSV aquí o haga clic para elegirlo</strong>
            <span class="caption">{{ nombre() || 'Ningún archivo seleccionado' }}</span>
            <input type="file" accept=".csv,text/csv" (change)="elegir($event)" hidden />
          </label>

          @if (res(); as r) {
            @if (r.errores.length) {
              <div class="aviso error" style="margin-top:16px">
                <app-icon name="alert" [size]="18" />
                <div><strong>No se cargó ningún registro.</strong> {{ filasConError(r) }} fila(s) con errores de {{ r.filas }}. Corrija el archivo y vuelva a subirlo.</div>
              </div>
              <div class="tabla-wrap" style="margin-top:12px;max-height:320px">
                <table class="tabla"><thead><tr><th class="num">Fila</th><th>Motivo</th></tr></thead>
                  <tbody>@for (e of r.errores; track $index) { <tr><td class="num">{{ e.fila }}</td><td>{{ e.motivo }}</td></tr> }</tbody>
                </table>
              </div>
            } @else if (!aplicado()) {
              <div class="aviso" style="margin-top:16px">
                <app-icon name="check" [size]="18" />
                <div><strong>Archivo válido:</strong> {{ r.filas }} fila(s) · {{ r.nuevos }} nuevo(s) · {{ r.actualizados }} {{ tipo() === 'disponibilidad' ? 'docente(s) con disponibilidad reemplazada' : 'actualizado(s)' }}. Separador detectado «{{ r.separador }}».</div>
              </div>
              <div class="tabla-wrap" style="margin-top:12px">
                <table class="tabla"><thead><tr>@for (c of r.vista[0]; track $index) { <th>{{ c }}</th> }</tr></thead>
                  <tbody>@for (f of r.vista.slice(1); track $index) { <tr>@for (c of f; track $index) { <td>{{ c }}</td> }</tr> }</tbody>
                </table>
              </div>
              @if (r.filas > 8) { <p class="caption" style="margin-top:6px">Vista previa de las primeras 8 filas.</p> }
              <div class="modal-foot">
                <button class="btn btn-ghost" (click)="limpiar()">Cancelar</button>
                <button class="btn btn-primary" (click)="confirmar(r)" [disabled]="store.ocupado()"><app-icon name="check" [size]="16" /> Confirmar importación</button>
              </div>
            } @else {
              <div class="aviso" style="margin-top:16px;background:#e3efd9">
                <app-icon name="check" [size]="18" />
                <div><strong>Importación aplicada.</strong> {{ r.resumen }}. Quedó registrada en la bitácora.</div>
              </div>
            }
          }
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .cols { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
      code { background: rgba(0,0,0,.06); border-radius: 4px; padding: 2px 6px; font-size: 12.5px; }
      .drop { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 32px 16px; border: 2px dashed var(--ink-20); border-radius: 12px;
        cursor: pointer; text-align: center; color: var(--ink-60); transition: border-color 200ms ease, background 200ms ease; }
      .drop:hover, .drop.over { border-color: var(--color-notion-blue); background: var(--color-sky-tint); }
    `,
  ],
})
export class Importar {
  protected readonly store = inject(StoreService);
  private readonly toast = inject(ToastService);
  protected readonly tipos: { valor: TipoImportacion; nombre: string }[] = [
    { valor: 'docentes', nombre: 'Docentes' },
    { valor: 'disponibilidad', nombre: 'Disponibilidad' },
    { valor: 'cursos', nombre: 'Cursos' },
    { valor: 'aulas', nombre: 'Aulas' },
  ];
  protected readonly tipo = signal<TipoImportacion>('docentes');
  protected readonly plantilla = computed(() => PLANTILLAS[this.tipo()]);
  protected readonly res = signal<ResultadoImportacion | null>(null);
  protected readonly nombre = signal('');
  protected readonly aplicado = signal(false);
  protected readonly sobre = signal(false);

  cambiarTipo(t: TipoImportacion) {
    this.tipo.set(t);
    this.limpiar();
  }
  limpiar() {
    this.res.set(null);
    this.nombre.set('');
    this.aplicado.set(false);
  }
  descargarPlantilla() {
    descargarArchivo(`plantilla_${this.tipo()}.csv`, plantillaCsv(this.tipo()));
  }
  filasConError(r: ResultadoImportacion) {
    return new Set(r.errores.map((e) => e.fila)).size;
  }

  elegir(ev: Event) {
    const f = (ev.target as HTMLInputElement).files?.[0];
    if (f) this.leer(f);
    (ev.target as HTMLInputElement).value = '';
  }
  soltar(ev: DragEvent) {
    ev.preventDefault();
    this.sobre.set(false);
    const f = ev.dataTransfer?.files?.[0];
    if (f) this.leer(f);
  }
  private leer(f: File) {
    f.text().then((t) => this.procesar(f.name, t));
  }
  private procesar(nombre: string, texto: string) {
    this.nombre.set(nombre);
    this.aplicado.set(false);
    this.res.set(procesarImportacion(this.tipo(), texto, { estado: this.store.estado(), periodoId: this.store.periodo().id, parametros: this.store.parametros() }));
  }
  async confirmar(r: ResultadoImportacion) {
    const res = await this.store.aplicarImportacion(r.cambios, `${this.nombre()} · ${r.resumen}`);
    if (!res.ok) {
      this.toast.error(res.mensaje ?? 'No se pudo aplicar la importación', res.errores);
      return;
    }
    this.aplicado.set(true);
    this.toast.ok('Importación aplicada', [r.resumen]);
  }
}
