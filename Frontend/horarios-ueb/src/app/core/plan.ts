import { Curso } from './models';

/** Las electivas profesionales del plan: una en 7.º, una en 8.º y una en 9.º semestre, de 2 créditos cada una. */
export const ELECTIVAS_PROFESIONALES_DEL_PLAN: { nivel: number; nombre: string; creditos: number }[] = [
  { nivel: 7, nombre: 'Electiva profesional I', creditos: 2 },
  { nivel: 8, nombre: 'Electiva profesional II', creditos: 2 },
  { nivel: 9, nombre: 'Electiva profesional III', creditos: 2 },
];

/** Créditos por semestre: cursos obligatorios y práctica del semestre más la electiva profesional que corresponda. */
export function creditosPorSemestre(cursos: Curso[]) {
  const out: { nivel: number; creditos: number; cursos: number; electiva: number }[] = [];
  for (let nivel = 1; nivel <= 9; nivel++) {
    const delNivel = cursos.filter((c) => c.nivel === nivel && c.tipo !== 'ELECTIVA_PROFESIONAL');
    const electiva = ELECTIVAS_PROFESIONALES_DEL_PLAN.filter((e) => e.nivel === nivel).reduce((t, e) => t + e.creditos, 0);
    out.push({ nivel, cursos: delNivel.length, electiva, creditos: delNivel.reduce((t, c) => t + c.creditos, 0) + electiva });
  }
  return out;
}

export function creditosDelPlan(cursos: Curso[]) {
  return creditosPorSemestre(cursos).reduce((t, s) => t + s.creditos, 0);
}
