package co.edu.unbosque.horariosuebback.herramientas;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.generador.GeneradorHorario;
import co.edu.unbosque.horariosuebback.domain.reglas.ValidadorReglas;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import co.edu.unbosque.horariosuebback.service.CargadorPeriodo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Herramienta (no es una prueba de la suite): genera BD/migraciones/V3__horario_2026_1.sql con el horario publicado de 2026-1,
 * usando el mismo generador del backend sobre los datos de V1 y V2.
 *
 * <pre>./gradlew test --tests "*GenerarHorarioSemillaTest" -DgenerarSemilla=true</pre>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:semilla;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.flyway.target=2"})
@EnabledIfSystemProperty(named = "generarSemilla", matches = "true")
class GenerarHorarioSemillaTest {

    private static final String PERIODO = "per-2026-1";
    private static final String[] DIAS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};

    @Autowired
    CargadorPeriodo cargador;
    @Autowired
    GeneradorHorario generador;
    @Autowired
    ValidadorReglas validador;

    @Test
    @Transactional
    void generar() throws Exception {
        CargadorPeriodo.Datos d = cargador.cargar(PERIODO);
        Catalogo cat = d.catalogo();
        int dur = d.parametros().duracionSesion();
        GeneradorHorario.Resultado r = generador.generar(new GeneradorHorario.Entrada(d.grupos(), cat.docentes(), cat.aulas(), d.parametros(), cat,
                List.of(), List.of()));

        // identificadores estables para la semilla
        List<Sesion> sesiones = new ArrayList<>(r.sesiones());
        sesiones.sort(Comparator.comparing(Sesion::getGrupoId).thenComparingInt(Sesion::getDia));
        for (int i = 0; i < sesiones.size(); i++) sesiones.get(i).setId(String.format("ses-2026-1-%04d", i + 1));
        sesiones.forEach(s -> s.setVersionId("ver-2026-1-v1"));
        var violaciones = validador.validarVersion(sesiones, d.parametros(), cat);
        assertEquals(0, violaciones.size(), () -> "violaciones: " + violaciones);

        // en el histórico, los grupos que no cupieron se dan por cerrados para que la versión sea publicable
        Set<String> cerrados = r.pendientes().stream().map(GeneradorHorario.Pendiente::grupoId).collect(Collectors.toSet());
        System.out.println("2026-1: " + sesiones.size() + " sesiones, " + r.milisegundos() + " ms, pendientes " + r.pendientes());
        for (Jornada j : Jornada.values()) {
            int[] porDia = new int[6];
            sesiones.stream().filter(s -> cat.grupo(s.getGrupoId()).getJornada() == j).forEach(s -> porDia[s.getDia()]++);
            StringBuilder sb = new StringBuilder("  " + j + ":");
            for (int i = 0; i < 6; i++) sb.append(' ').append(DIAS[i]).append('=').append(porDia[i]);
            System.out.println(sb);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("""
                -- =====================================================================================================
                -- Horarios UEB · Horario publicado de 2026-1 (datos semilla)
                --
                -- Generado con el algoritmo del backend (GeneradorVoraz) sobre los datos de V2 y validado contra R1–R9
                -- (0 violaciones). Para regenerarlo: ./gradlew test --tests "*GenerarHorarioSemillaTest" -DgenerarSemilla=true
                -- =====================================================================================================

                INSERT INTO version_horario (id, periodo_id, numero, estado, origen, fecha_creacion, fecha_publicacion, autor) VALUES
                    ('ver-2026-1-v1', 'per-2026-1', 1, 'PUBLICADA', 'Generada automáticamente (histórico)', '2026-01-12 14:00:00.000', '2026-01-20 15:00:00.000', 'Diana Marcela Rojas');

                """);
        for (int i = 0; i < sesiones.size(); i += 100) {
            sql.append("INSERT INTO sesion (id, version_id, grupo_id, dia, inicio, docente_id, aula_id) VALUES\n");
            sql.append(sesiones.subList(i, Math.min(i + 100, sesiones.size())).stream()
                    .map(s -> String.format("    ('%s', '%s', '%s', %d, %d, '%s', '%s')", s.getId(), s.getVersionId(), s.getGrupoId(), s.getDia(), s.getInicio(),
                            s.getDocenteId(), s.getAulaId()))
                    .collect(Collectors.joining(",\n")));
            sql.append(";\n\n");
        }
        sql.append("-- inscritos al cierre del periodo\n");
        sql.append("UPDATE grupo SET inscritos = cupo_planeado - 4 WHERE periodo_id = 'per-2026-1';\n");
        for (String g : cerrados.stream().sorted().toList()) {
            sql.append("UPDATE grupo SET estado = 'CERRADO' WHERE id = '").append(g).append("';\n");
        }

        // horario de atención de Germán Augusto Neuta en 2026-1: una hora libre del viernes para Legislación y «acordar» para su electiva
        Docente german = cat.docentes().stream().filter(x -> x.getNombre().equals("Germán Augusto Neuta")).findFirst().orElseThrow();
        List<Sesion> suyas = sesiones.stream().filter(s -> german.getId().equals(s.getDocenteId())).toList();
        Curso legislacion = d.grupos().stream().map(g -> cat.curso(g.getCursoId())).filter(c -> c.getNombre().startsWith("Legislación")).findFirst().orElseThrow();
        int libre = java.util.stream.IntStream.of(12, 11, 17, 15, 7)
                .filter(h -> suyas.stream().noneMatch(s -> s.getDia() == 4 && s.getInicio() < h + 1 && h < s.getInicio() + dur))
                .findFirst().orElse(12);
        sql.append("\n-- horario de atención de 2026-1 (Germán Augusto Neuta)\n");
        sql.append("INSERT INTO horario_atencion (id, periodo_id, docente_id, curso_id, modo, dia, inicio, fin, lugar) VALUES\n");
        List<String> filas = new ArrayList<>();
        filas.add(String.format("    ('ate-2026-1-1', 'per-2026-1', '%s', '%s', 'HORARIO', 4, %d, %d, 'Sala de profesores, bloque A piso 3')",
                german.getId(), legislacion.getId(), libre, libre + 1));
        suyas.stream().map(s -> cat.grupo(s.getGrupoId())).map(Grupo::getCursoId).filter(c -> !c.equals(legislacion.getId())).findFirst()
                .ifPresent(c -> filas.add(String.format("    ('ate-2026-1-2', 'per-2026-1', '%s', '%s', 'ACORDAR', NULL, NULL, NULL, 'Escribir a %s')",
                        german.getId(), c, german.getCorreoInstitucional())));
        sql.append(String.join(",\n", filas)).append(";\n\n");

        sql.append("INSERT INTO bitacora (fecha, usuario, rol, accion, detalle) VALUES\n");
        sql.append("    ('2026-01-12 14:00:00.000', 'Diana Marcela Rojas', 'COORDINADOR', 'Generación de propuesta', 'Versión 1 de 2026-1: ")
                .append(sesiones.size()).append(" sesiones'),\n");
        sql.append("    ('2026-01-20 15:00:00.000', 'Diana Marcela Rojas', 'COORDINADOR', 'Publicación de horario', 'Versión 1 de 2026-1 publicada como oficial');\n");

        Path destino = Path.of("../../BD/migraciones/V3__horario_2026_1.sql");
        Files.writeString(destino, sql.toString(), StandardCharsets.UTF_8);
        System.out.println("Escrito " + destino.toAbsolutePath().normalize());

        // vista previa de 2026-2 (no se escribe): comprueba el balance de la semana del periodo abierto
        CargadorPeriodo.Datos d2 = cargador.cargar("per-2026-2");
        GeneradorHorario.Resultado r2 = generador.generar(new GeneradorHorario.Entrada(d2.grupos(), d2.catalogo().docentes(), d2.catalogo().aulas(),
                d2.parametros(), d2.catalogo(), List.of(), List.of()));
        System.out.println("2026-2 (vista previa): " + r2.sesiones().size() + " sesiones, " + r2.milisegundos() + " ms, pendientes " + r2.pendientes());
        for (Jornada j : Jornada.values()) {
            int[] porDia = new int[6];
            r2.sesiones().stream().filter(s -> d2.catalogo().grupo(s.getGrupoId()).getJornada() == j).forEach(s -> porDia[s.getDia()]++);
            StringBuilder sb = new StringBuilder("  " + j + ":");
            for (int i = 0; i < 6; i++) sb.append(' ').append(DIAS[i]).append('=').append(porDia[i]);
            System.out.println(sb);
        }
    }
}
