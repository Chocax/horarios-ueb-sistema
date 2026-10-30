package co.edu.unbosque.horariosuebback;

import co.edu.unbosque.horariosuebback.domain.generador.GeneradorHorario;
import co.edu.unbosque.horariosuebback.domain.reglas.Asignacion;
import co.edu.unbosque.horariosuebback.domain.reglas.ValidadorReglas;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import co.edu.unbosque.horariosuebback.exception.NegocioException;
import co.edu.unbosque.horariosuebback.service.AuthService;
import co.edu.unbosque.horariosuebback.service.CargadorPeriodo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de integración sobre H2 en memoria con el esquema y los datos semilla reales (Flyway). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pruebas;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@Transactional
class HorariosUebBackApplicationTests {

    @Autowired
    CargadorPeriodo cargador;
    @Autowired
    GeneradorHorario generador;
    @Autowired
    ValidadorReglas validador;
    @Autowired
    AuthService auth;
    @Autowired
    co.edu.unbosque.horariosuebback.repository.UsuarioRepository usuarios;
    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder encoder;

    private GeneradorHorario.Resultado generar(CargadorPeriodo.Datos d) {
        return generador.generar(new GeneradorHorario.Entrada(d.grupos(), d.catalogo().docentes(), d.catalogo().aulas(), d.parametros(),
                d.catalogo(), List.of(), List.of()));
    }

    @Test
    void elGeneradorUbicaTodos2026_2SinViolaciones() {
        CargadorPeriodo.Datos d = cargador.cargar("per-2026-2");
        GeneradorHorario.Resultado r = generar(d);
        assertTrue(r.pendientes().isEmpty(), () -> "pendientes: " + r.pendientes());
        assertEquals(0, validador.validarVersion(r.sesiones(), d.parametros(), d.catalogo()).size());
        // Fundamentos de programación se ve 3 veces por semana
        long fundamentosG1 = r.sesiones().stream().filter(s -> s.getGrupoId().equals("grp-2026-2-isi-104-g1")).count();
        assertEquals(3, fundamentosG1);
    }

    @Test
    void elViernesNoQuedaDescargado() {
        CargadorPeriodo.Datos d = cargador.cargar("per-2026-2");
        GeneradorHorario.Resultado r = generar(d);
        int[] porDia = new int[6];
        r.sesiones().stream().filter(s -> d.catalogo().grupo(s.getGrupoId()).getJornada() == Jornada.DIURNA).forEach(s -> porDia[s.getDia()]++);
        double promedio = Arrays.stream(porDia, 0, 5).average().orElse(0);
        assertTrue(porDia[4] >= promedio * 0.85, () -> "viernes " + porDia[4] + " vs promedio " + promedio);
    }

    @Test
    void elHorarioSemilla2026_1NoTieneViolaciones() {
        CargadorPeriodo.Datos d = cargador.cargar("per-2026-1");
        List<Sesion> sesiones = cargador.sesionesVigentes("per-2026-1");
        assertTrue(sesiones.size() > 250);
        assertEquals(0, validador.validarVersion(sesiones, d.parametros(), d.catalogo()).size());
    }

    @Test
    void cadaReglaRechazaYSeNombra() {
        CargadorPeriodo.Datos d = cargador.cargar("per-2026-1");
        List<Sesion> sesiones = cargador.sesionesVigentes("per-2026-1");
        Sesion s = sesiones.get(0);
        // almuerzo: R3
        var r3 = validador.validar(new Asignacion(s.getId(), s.getGrupoId(), s.getDia(), 13, s.getDocenteId(), s.getAulaId()), sesiones, d.parametros(), d.catalogo());
        assertTrue(r3.stream().anyMatch(v -> v.regla().equals("R3")));
        // misma franja que otra sesión del mismo docente: R1
        Sesion otra = sesiones.stream().filter(x -> x.getDocenteId().equals(s.getDocenteId()) && !x.getGrupoId().equals(s.getGrupoId())).findFirst().orElseThrow();
        var r1 = validador.validar(new Asignacion(s.getId(), s.getGrupoId(), otra.getDia(), otra.getInicio(), s.getDocenteId(), s.getAulaId()), sesiones,
                d.parametros(), d.catalogo());
        assertTrue(r1.stream().anyMatch(v -> v.regla().equals("R1")));
        // sin aula: R4
        var r4 = validador.validar(new Asignacion(s.getId(), s.getGrupoId(), s.getDia(), s.getInicio(), s.getDocenteId(), null), sesiones, d.parametros(), d.catalogo());
        assertTrue(r4.stream().anyMatch(v -> v.regla().equals("R4")));
    }

    @Test
    void loginConBcryptYRechazoDeClaveErrada() {
        var admin = usuarios.findByCorreoIgnoreCase("admin@unibosque.com").orElseThrow();
        admin.setHashClave(encoder.encode("PruebaSegura2026"));
        var r = auth.login("admin@unibosque.com", "PruebaSegura2026", "127.0.0.1");
        assertNotNull(r.token());
        assertThrows(NegocioException.class, () -> auth.login("admin@unibosque.com", "otra", "127.0.0.1"));
    }

    @Test
    void politicaDeContrasenas() {
        assertTrue(AuthService.validarClave("corta1").size() > 0);
        assertTrue(AuthService.validarClave("sinnumeros").size() > 0);
        assertEquals(0, AuthService.validarClave("Horarios2026").size());
    }
}
