import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { TemaService } from '../core/services/tema.service';
import { Icon } from './icon';

/** Botón para cambiar entre el modo claro y el oscuro. */
@Component({
  selector: 'app-boton-tema',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button class="btn icon-btn" type="button" (click)="tema.alternar()" [attr.aria-label]="etiqueta()" [title]="etiqueta()">
      <app-icon [name]="tema.actual() === 'oscuro' ? 'sun' : 'moon'" [size]="18" />
    </button>
  `,
  styles: [':host{display:inline-flex;flex:none}'],
})
export class BotonTema {
  protected readonly tema = inject(TemaService);
  protected readonly etiqueta = computed(() => (this.tema.actual() === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'));
}
