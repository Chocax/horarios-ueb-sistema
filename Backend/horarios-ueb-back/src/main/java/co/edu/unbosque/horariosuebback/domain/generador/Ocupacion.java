package co.edu.unbosque.horariosuebback.domain.generador;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Índices de ocupación para descartar candidatos rápido: docente, aula, horario de cada semestre y carga por día. */
class Ocupacion {

    private record Inicio(int inicio, String cursoId) {
    }

    private final Catalogo catalogo;
    private final Parametros p;
    private final int dur;
    private final Set<String> docente = new HashSet<>();
    private final Set<String> aula = new HashSet<>();
    /** nivel|jornada|día → horas de inicio con su curso */
    private final Map<String, List<Inicio>> nivel = new HashMap<>();
    private final Map<String, List<Integer>> docenteDia = new HashMap<>();
    /** sesiones por jornada y día (0 = lunes … 5 = sábado), para repartir la semana */
    private final Map<Jornada, int[]> porDia = new EnumMap<>(Jornada.class);
    /** horas de clase asignadas a cada aula, para repartir la ocupación */
    private final Map<String, Integer> usoAula = new HashMap<>();

    Ocupacion(Catalogo catalogo, Parametros p) {
        this.catalogo = catalogo;
        this.p = p;
        this.dur = p.duracionSesion();
        for (Jornada j : Jornada.values()) porDia.put(j, new int[6]);
    }

    void agregar(Sesion s) {
        for (int h = s.getInicio(); h < s.getInicio() + dur; h++) {
            if (s.getDocenteId() != null) docente.add(s.getDocenteId() + "|" + s.getDia() + "|" + h);
            if (s.getAulaId() != null) aula.add(s.getAulaId() + "|" + s.getDia() + "|" + h);
        }
        if (s.getAulaId() != null) usoAula.merge(s.getAulaId(), dur, Integer::sum);
        Grupo g = catalogo.grupo(s.getGrupoId());
        Curso c = g == null ? null : catalogo.curso(g.getCursoId());
        if (g != null && c != null) {
            nivel.computeIfAbsent(c.getNivel() + "|" + g.getJornada() + "|" + s.getDia(), k -> new ArrayList<>()).add(new Inicio(s.getInicio(), c.getId()));
            porDia.get(g.getJornada())[s.getDia()]++;
        }
        if (s.getDocenteId() != null) {
            docenteDia.computeIfAbsent(s.getDocenteId() + "|" + s.getDia(), k -> new ArrayList<>()).add(s.getInicio());
        }
    }

    /** Marca horas del docente como no disponibles para clase (horario de atención). */
    void bloquearDocente(String id, int dia, int inicio, int fin) {
        for (int h = inicio; h < fin; h++) docente.add(id + "|" + dia + "|" + h);
    }

    boolean docenteOcupado(String id, int dia, int h) {
        for (int x = h; x < h + dur; x++) if (docente.contains(id + "|" + dia + "|" + x)) return true;
        return false;
    }

    boolean aulaOcupada(String id, int dia, int h) {
        for (int x = h; x < h + dur; x++) if (aula.contains(id + "|" + dia + "|" + x)) return true;
        return false;
    }

    record EfectoNivel(boolean cruza, int huecos, int sobrecarga) {
    }

    /** Efecto de agregar una sesión al horario del semestre en esa jornada: cruces, huecos y días recargados. */
    EfectoNivel efectoNivel(int niv, Jornada jornada, String cursoId, int dia, int h) {
        List<Inicio> l = nivel.getOrDefault(niv + "|" + jornada + "|" + dia, List.of());
        boolean cruza = l.stream().anyMatch(x -> !x.cursoId().equals(cursoId) && x.inicio() < h + dur && h < x.inicio() + dur);
        List<Integer> inicios = new ArrayList<>(l.stream().map(Inicio::inicio).toList());
        inicios.add(h);
        inicios.sort(Integer::compare);
        int huecos = 0;
        for (int i = 1; i < inicios.size(); i++) {
            int ini = inicios.get(i - 1) + dur;
            int libre = inicios.get(i) - ini;
            // el bloque no asignable (almuerzo) no cuenta como hueco
            int fin = inicios.get(i);
            int almuerzo = p.bloquesNoAsignables().stream().mapToInt(b -> Math.max(0, Math.min(b.fin(), fin) - Math.max(b.inicio(), ini))).sum();
            if (libre - almuerzo > 0) huecos += (int) Math.ceil((libre - almuerzo) / (double) dur);
        }
        return new EfectoNivel(cruza, huecos, l.size() >= 3 ? 1 : 0);
    }

    /** 0 si el docente ya tiene clase cerca ese día (sin huecos), 1 si quedaría un hueco, 0.5 si es su primera clase del día. */
    double huecoDocente(String id, int dia, int h) {
        List<Integer> l = docenteDia.get(id + "|" + dia);
        if (l == null || l.isEmpty()) return 0.5;
        int dist = l.stream().mapToInt(x -> Math.abs(x - h)).min().orElse(0);
        return dist <= dur ? 0 : 1;
    }

    /**
     * Desbalance del día entre lunes y viernes para la jornada: negativo si el día va por debajo del promedio y positivo si
     * va por encima. Hace que el generador reparta la semana y no deje el viernes casi vacío.
     */
    double desbalanceDia(Jornada jornada, int dia) {
        if (dia == 5) return 0;
        int[] c = porDia.get(jornada);
        double prom = (c[0] + c[1] + c[2] + c[3] + c[4]) / 5.0;
        return (c[dia] - prom) / Math.max(prom, 4);
    }

    int horasAula(String id) {
        return usoAula.getOrDefault(id, 0);
    }

    int sesionesDia(Jornada jornada, int dia) {
        return porDia.get(jornada)[dia];
    }
}
