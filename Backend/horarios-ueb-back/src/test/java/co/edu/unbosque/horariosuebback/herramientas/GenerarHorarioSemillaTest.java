package co.edu.unbosque.horariosuebback.herramientas;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.generador.GeneradorHorario;
import co.edu.unbosque.horariosuebback.domain.reglas.ValidadorReglas;
import co.edu.unbosque.horariosuebback.entity.Aula;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Herramienta (no es una prueba de la suite): genera BD/migraciones/V4__aulas_y_horario_2026_1.sql. Aplica a los cursos las
 * características de aula corregidas y reemplaza el horario publicado de 2026-1 por uno generado con el algoritmo actual
 * (sábado diurno solo como último recurso y ocupación de aulas repartida).
 *
 * <pre>./gradlew test --tests "*GenerarHorarioSemillaTest" -DgenerarSemilla=true</pre>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:semilla;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.flyway.target=3"})
@EnabledIfSystemProperty(named = "generarSemilla", matches = "true")
class GenerarHorarioSemillaTest {

    private static final String PERIODO = "per-2026-1";
    private static final String VERSION = "ver-2026-1-v1";
    private static final String[] DIAS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};

    /** Cursos que pasan a requerir sala de cómputo. */
    private static final String CAMBIOS_AULAS = """
            -- Sistemas digitales y arquitectura de computador, Dinámica de sistemas y Modelos y simulación de sistemas usan
            -- software (simuladores, herramientas de diseño digital): pasan a sala de cómputo.
            INSERT INTO curso_caracteristica (curso_id, caracteristica) VALUES
                ('cur-isi-404', 'EQUIPOS_COMPUTO'),
                ('cur-isi-501', 'EQUIPOS_COMPUTO'),
                ('cur-isi-602', 'EQUIPOS_COMPUTO');
            -- las salas de cómputo son de 35 puestos como máximo: el cupo de sus grupos se ajusta
            UPDATE grupo SET cupo_planeado = 35 WHERE curso_id IN ('cur-isi-404', 'cur-isi-501', 'cur-isi-602') AND cupo_planeado > 35;
            UPDATE grupo SET inscritos = cupo_planeado - 4 WHERE periodo_id = 'per-2026-1' AND inscritos > cupo_planeado;
            """;

    @Autowired
    CargadorPeriodo cargador;
    @Autowired
    GeneradorHorario generador;
    @Autowired
    ValidadorReglas validador;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Test
    @Transactional
    void generar() throws Exception {
        jdbc.execute(CAMBIOS_AULAS.lines().filter(l -> !l.startsWith("--")).collect(Collectors.joining("\n")));
        CargadorPeriodo.Datos d = cargador.cargar(PERIODO);
        Catalogo cat = d.catalogo();
        int dur = d.parametros().duracionSesion();
        GeneradorHorario.Resultado r = generador.generar(new GeneradorHorario.Entrada(d.grupos(), cat.docentes(), cat.aulas(), d.parametros(), cat,
                List.of(), List.of()));

        List<Sesion> sesiones = new ArrayList<>(r.sesiones());
        sesiones.sort(Comparator.comparing(Sesion::getGrupoId).thenComparingInt(Sesion::getDia));
        for (int i = 0; i < sesiones.size(); i++) sesiones.get(i).setId(String.format("ses-2026-1-%04d", i + 1));
        sesiones.forEach(s -> s.setVersionId(VERSION));
        var violaciones = validador.validarVersion(sesiones, d.parametros(), cat);
        assertEquals(0, violaciones.size(), () -> "violaciones: " + violaciones);
        Set<String> cerrados = r.pendientes().stream().map(GeneradorHorario.Pendiente::grupoId).collect(Collectors.toSet());
        resumen("2026-1", sesiones, r, cat);

        StringBuilder sql = new StringBuilder("""
                -- =====================================================================================================
                -- Horarios UEB · Ajustes de aulas y nuevo horario publicado de 2026-1
                --
                -- 1. Cursos que pasan a requerir sala de cómputo.
                -- 2. Usuarios para todos los docentes (al final del script).
                -- 3. El horario publicado de 2026-1 se reemplaza por uno generado con el algoritmo actual: los grupos
                --    diurnos no tienen clase el sábado salvo que no haya otra opción y la ocupación de las aulas queda
                --    repartida. Validado contra R1–R9 (0 violaciones).
                -- Para regenerarlo: ./gradlew test --tests "*GenerarHorarioSemillaTest" -DgenerarSemilla=true
                -- =====================================================================================================

                """);
        sql.append(CAMBIOS_AULAS).append("\n");
        sql.append("DELETE FROM sesion WHERE version_id = '").append(VERSION).append("';\n\n");
        for (int i = 0; i < sesiones.size(); i += 100) {
            sql.append("INSERT INTO sesion (id, version_id, grupo_id, dia, inicio, docente_id, aula_id) VALUES\n");
            sql.append(sesiones.subList(i, Math.min(i + 100, sesiones.size())).stream()
                    .map(s -> String.format("    ('%s', '%s', '%s', %d, %d, '%s', '%s')", s.getId(), s.getVersionId(), s.getGrupoId(), s.getDia(), s.getInicio(),
                            s.getDocenteId(), s.getAulaId()))
                    .collect(Collectors.joining(",\n")));
            sql.append(";\n\n");
        }
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
        sql.append("\n-- horario de atención de 2026-1 (Germán Augusto Neuta), acorde con el nuevo horario\n");
        sql.append("DELETE FROM horario_atencion WHERE periodo_id = 'per-2026-1';\n");
        sql.append("INSERT INTO horario_atencion (id, periodo_id, docente_id, curso_id, modo, dia, inicio, fin, lugar) VALUES\n");
        List<String> filas = new ArrayList<>();
        filas.add(String.format("    ('ate-2026-1-1', 'per-2026-1', '%s', '%s', 'HORARIO', 4, %d, %d, 'Sala de profesores, bloque A piso 3')",
                german.getId(), legislacion.getId(), libre, libre + 1));
        suyas.stream().map(s -> cat.grupo(s.getGrupoId())).map(Grupo::getCursoId).filter(c -> !c.equals(legislacion.getId())).findFirst()
                .ifPresent(c -> filas.add(String.format("    ('ate-2026-1-2', 'per-2026-1', '%s', '%s', 'ACORDAR', NULL, NULL, NULL, 'Escribir a %s')",
                        german.getId(), c, german.getCorreoInstitucional())));
        sql.append(String.join(",\n", filas)).append(";\n");

        // usuarios para los docentes que aún no tienen: la clave inicial llega por -DclaveInicial (solo se escribe su hash)
        String clave = System.getProperty("claveInicial", "");
        if (clave.isBlank()) throw new IllegalStateException("Pase la clave inicial de los docentes con -DclaveInicial=...");
        sql.append("""

                -- usuarios para todos los docentes que aún no tienen cuenta (rol DOCENTE, vinculados a su docente). Usan su correo
                -- institucional y la misma contraseña inicial que los demás usuarios (solo se guarda su hash BCrypt).
                INSERT INTO usuario (id, nombre, correo, hash_clave, rol, activo, docente_id)
                SELECT CONCAT('usr-', LOWER(d.identificador)), d.nombre, LOWER(d.correo_institucional), '%s', 'DOCENTE', TRUE, d.id
                FROM docente d
                WHERE d.correo_institucional <> ''
                  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.docente_id = d.id OR LOWER(u.correo) = LOWER(d.correo_institucional))
                  AND (SELECT COUNT(*) FROM docente d2 WHERE LOWER(d2.correo_institucional) = LOWER(d.correo_institucional)) = 1;
                """.formatted(encoder.encode(clave)));

        Path destino = Path.of("../../BD/migraciones/V4__aulas_y_horario_2026_1.sql");
        Files.writeString(destino, sql.toString(), StandardCharsets.UTF_8);
        System.out.println("Escrito " + destino.toAbsolutePath().normalize());

        // vista previa de 2026-2 (no se escribe)
        CargadorPeriodo.Datos d2 = cargador.cargar("per-2026-2");
        GeneradorHorario.Resultado r2 = generador.generar(new GeneradorHorario.Entrada(d2.grupos(), d2.catalogo().docentes(), d2.catalogo().aulas(),
                d2.parametros(), d2.catalogo(), List.of(), List.of()));
        resumen("2026-2 (vista previa)", r2.sesiones(), r2, d2.catalogo());
    }

    private void resumen(String titulo, List<Sesion> sesiones, GeneradorHorario.Resultado r, Catalogo cat) {
        System.out.println(titulo + ": " + sesiones.size() + " sesiones, " + r.milisegundos() + " ms, pendientes " + r.pendientes());
        for (Jornada j : Jornada.values()) {
            int[] porDia = new int[6];
            sesiones.stream().filter(s -> cat.grupo(s.getGrupoId()).getJornada() == j).forEach(s -> porDia[s.getDia()]++);
            StringBuilder sb = new StringBuilder("  " + j + ":");
            for (int i = 0; i < 6; i++) sb.append(' ').append(DIAS[i]).append('=').append(porDia[i]);
            System.out.println(sb);
        }
        Map<String, Long> uso = sesiones.stream().collect(Collectors.groupingBy(Sesion::getAulaId, Collectors.counting()));
        IntSummaryStatistics est = cat.aulas().stream().mapToInt(a -> uso.getOrDefault(a.getId(), 0L).intValue()).summaryStatistics();
        long sinUso = cat.aulas().stream().filter(a -> !uso.containsKey(a.getId())).count();
        System.out.println("  aulas: sesiones por aula min=" + est.getMin() + " max=" + est.getMax() + " prom=" + String.format("%.1f", est.getAverage())
                + " · sin uso=" + sinUso + " de " + est.getCount());
        long malUsadas = sesiones.stream().filter(s -> {
            Aula a = cat.aula(s.getAulaId());
            Curso c = cat.cursoDeGrupo(s.getGrupoId());
            return !a.getCaracteristicas().isEmpty() && a.getCaracteristicas().stream().noneMatch(c.getCaracteristicasRequeridas()::contains);
        }).count();
        System.out.println("  sesiones en aula especial que el curso no necesita (B5): " + malUsadas);
    }
}
