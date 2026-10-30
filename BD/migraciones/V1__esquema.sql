-- =====================================================================================================
-- Horarios UEB · Esquema de la base de datos (MySQL 8)
-- Programa de Ingeniería de Sistemas · Universidad El Bosque
--
-- Lo ejecuta Flyway al arrancar el backend (una sola vez por base de datos). También se puede correr a
-- mano en un cliente MySQL. Compatible con H2 en modo MySQL (perfil local del backend).
-- Todas las tablas tienen llave primaria (requisito de Aiven: sql_require_primary_key).
-- =====================================================================================================

-- ------------------------------------------------------------------ periodos y parámetros (RF-01, RF-02, RF-16)
CREATE TABLE periodo_academico (
    id                     VARCHAR(40)  NOT NULL,
    codigo                 VARCHAR(10)  NOT NULL,
    anio                   INT          NOT NULL,
    semestre               INT          NOT NULL,
    estado                 VARCHAR(10)  NOT NULL,
    franja_lv_inicio       INT          NOT NULL,
    franja_lv_fin          INT          NOT NULL,
    franja_sab_inicio      INT          NOT NULL,
    franja_sab_fin         INT          NOT NULL,
    duracion_sesion        INT          NOT NULL,
    cupo_maximo            INT          NOT NULL,
    tolerancia_cupo        INT          NOT NULL,
    cupo_minimo            INT          NOT NULL,
    sesiones_minimas       INT          NOT NULL,
    sesiones_maximas       INT          NOT NULL,
    horas_tiempo_completo  INT          NOT NULL,
    horas_tres_cuartos     INT          NOT NULL,
    horas_medio_tiempo     INT          NOT NULL,
    horas_cuarto_tiempo    INT          NOT NULL,
    CONSTRAINT pk_periodo PRIMARY KEY (id),
    CONSTRAINT uk_periodo_codigo UNIQUE (codigo),
    CONSTRAINT ck_periodo_semestre CHECK (semestre IN (1, 2)),
    CONSTRAINT ck_periodo_estado CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

-- Bloques no asignables del periodo (por defecto el almuerzo, 13:00–14:00)
CREATE TABLE bloque_no_asignable (
    periodo_id  VARCHAR(40) NOT NULL,
    orden       INT         NOT NULL,
    inicio      INT         NOT NULL,
    fin         INT         NOT NULL,
    CONSTRAINT pk_bloque_no_asignable PRIMARY KEY (periodo_id, orden),
    CONSTRAINT fk_bloque_periodo FOREIGN KEY (periodo_id) REFERENCES periodo_academico (id) ON DELETE CASCADE,
    CONSTRAINT ck_bloque_rango CHECK (inicio < fin)
);

-- ------------------------------------------------------------------ catálogo académico (RF-03 a RF-06)
CREATE TABLE curso (
    id                  VARCHAR(40)  NOT NULL,
    codigo              VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    nivel               INT          NOT NULL,
    creditos            INT          NOT NULL,
    sesiones_semanales  INT          NOT NULL,
    tipo                VARCHAR(25)  NOT NULL,
    CONSTRAINT pk_curso PRIMARY KEY (id),
    CONSTRAINT uk_curso_codigo UNIQUE (codigo),
    CONSTRAINT ck_curso_tipo CHECK (tipo IN ('OBLIGATORIA', 'ELECTIVA_PROFESIONAL', 'PRACTICA')),
    CONSTRAINT ck_curso_nivel CHECK (nivel BETWEEN 1 AND 10),
    CONSTRAINT ck_curso_creditos CHECK (creditos BETWEEN 0 AND 12)
);

CREATE TABLE curso_caracteristica (
    curso_id        VARCHAR(40) NOT NULL,
    caracteristica  VARCHAR(25) NOT NULL,
    CONSTRAINT pk_curso_caracteristica PRIMARY KEY (curso_id, caracteristica),
    CONSTRAINT fk_curso_car_curso FOREIGN KEY (curso_id) REFERENCES curso (id) ON DELETE CASCADE
);

CREATE TABLE docente (
    id                   VARCHAR(40)  NOT NULL,
    identificador        VARCHAR(20)  NOT NULL,
    nombre               VARCHAR(120) NOT NULL,
    correo_institucional VARCHAR(120) NOT NULL,
    tipo_vinculacion     VARCHAR(20)  NOT NULL,
    categoria_escalafon  VARCHAR(40)  NOT NULL,
    horas_max_docencia   INT          NOT NULL,
    CONSTRAINT pk_docente PRIMARY KEY (id),
    CONSTRAINT uk_docente_identificador UNIQUE (identificador),
    CONSTRAINT ck_docente_vinculacion CHECK (tipo_vinculacion IN ('TIEMPO_COMPLETO', 'TRES_CUARTOS', 'MEDIO_TIEMPO', 'CUARTO_TIEMPO')),
    CONSTRAINT ck_docente_horas CHECK (horas_max_docencia >= 0)
);

-- Cursos que el docente está habilitado para orientar (R5)
CREATE TABLE docente_habilitacion (
    docente_id  VARCHAR(40) NOT NULL,
    curso_id    VARCHAR(40) NOT NULL,
    CONSTRAINT pk_docente_habilitacion PRIMARY KEY (docente_id, curso_id),
    CONSTRAINT fk_hab_docente FOREIGN KEY (docente_id) REFERENCES docente (id) ON DELETE CASCADE,
    CONSTRAINT fk_hab_curso FOREIGN KEY (curso_id) REFERENCES curso (id) ON DELETE CASCADE
);

-- Disponibilidad semanal del docente (R6). dia: 0 = lunes … 5 = sábado
CREATE TABLE docente_disponibilidad (
    docente_id  VARCHAR(40) NOT NULL,
    dia         INT         NOT NULL,
    inicio      INT         NOT NULL,
    fin         INT         NOT NULL,
    CONSTRAINT pk_docente_disponibilidad PRIMARY KEY (docente_id, dia, inicio),
    CONSTRAINT fk_disp_docente FOREIGN KEY (docente_id) REFERENCES docente (id) ON DELETE CASCADE,
    CONSTRAINT ck_disp_dia CHECK (dia BETWEEN 0 AND 5),
    CONSTRAINT ck_disp_rango CHECK (inicio < fin)
);

CREATE TABLE aula (
    id         VARCHAR(40)  NOT NULL,
    codigo     VARCHAR(20)  NOT NULL,
    nombre     VARCHAR(120) NOT NULL,
    capacidad  INT          NOT NULL,
    bloque     VARCHAR(5),
    piso       INT,
    CONSTRAINT pk_aula PRIMARY KEY (id),
    CONSTRAINT uk_aula_codigo UNIQUE (codigo),
    CONSTRAINT ck_aula_capacidad CHECK (capacidad > 0)
);

CREATE TABLE aula_caracteristica (
    aula_id         VARCHAR(40) NOT NULL,
    caracteristica  VARCHAR(25) NOT NULL,
    CONSTRAINT pk_aula_caracteristica PRIMARY KEY (aula_id, caracteristica),
    CONSTRAINT fk_aula_car_aula FOREIGN KEY (aula_id) REFERENCES aula (id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------ oferta y grupos (RF-07, RF-18)
CREATE TABLE oferta_curso (
    periodo_id        VARCHAR(40) NOT NULL,
    curso_id          VARCHAR(40) NOT NULL,
    demanda_estimada  INT         NOT NULL,
    CONSTRAINT pk_oferta_curso PRIMARY KEY (periodo_id, curso_id),
    CONSTRAINT fk_oferta_periodo FOREIGN KEY (periodo_id) REFERENCES periodo_academico (id) ON DELETE CASCADE,
    CONSTRAINT fk_oferta_curso FOREIGN KEY (curso_id) REFERENCES curso (id) ON DELETE CASCADE,
    CONSTRAINT ck_oferta_demanda CHECK (demanda_estimada >= 0)
);

CREATE TABLE grupo (
    id                    VARCHAR(40) NOT NULL,
    periodo_id            VARCHAR(40) NOT NULL,
    curso_id              VARCHAR(40) NOT NULL,
    codigo                VARCHAR(10) NOT NULL,
    jornada               VARCHAR(10) NOT NULL,
    cupo_planeado         INT         NOT NULL,
    inscritos             INT,
    estado                VARCHAR(10) NOT NULL,
    docente_preferido_id  VARCHAR(40),
    CONSTRAINT pk_grupo PRIMARY KEY (id),
    CONSTRAINT uk_grupo_codigo UNIQUE (periodo_id, curso_id, codigo),
    CONSTRAINT fk_grupo_periodo FOREIGN KEY (periodo_id) REFERENCES periodo_academico (id),
    CONSTRAINT fk_grupo_curso FOREIGN KEY (curso_id) REFERENCES curso (id),
    CONSTRAINT fk_grupo_docente_pref FOREIGN KEY (docente_preferido_id) REFERENCES docente (id) ON DELETE SET NULL,
    CONSTRAINT ck_grupo_jornada CHECK (jornada IN ('DIURNA', 'NOCTURNA')),
    CONSTRAINT ck_grupo_estado CHECK (estado IN ('ABIERTO', 'CERRADO')),
    CONSTRAINT ck_grupo_cupo CHECK (cupo_planeado > 0),
    CONSTRAINT ck_grupo_inscritos CHECK (inscritos IS NULL OR inscritos >= 0)
);

-- ------------------------------------------------------------------ versiones del horario (RF-08 a RF-12)
CREATE TABLE version_horario (
    id                 VARCHAR(40)  NOT NULL,
    periodo_id         VARCHAR(40)  NOT NULL,
    numero             INT          NOT NULL,
    estado             VARCHAR(10)  NOT NULL,
    origen             VARCHAR(200) NOT NULL,
    fecha_creacion     DATETIME(3)  NOT NULL,
    fecha_publicacion  DATETIME(3),
    autor              VARCHAR(120) NOT NULL,
    CONSTRAINT pk_version_horario PRIMARY KEY (id),
    CONSTRAINT uk_version_numero UNIQUE (periodo_id, numero),
    CONSTRAINT fk_version_periodo FOREIGN KEY (periodo_id) REFERENCES periodo_academico (id),
    CONSTRAINT ck_version_estado CHECK (estado IN ('BORRADOR', 'PUBLICADA', 'ARCHIVADA'))
);

-- Sesión de clase (bloque de duracion_sesion horas). Las restricciones UNIQUE son la defensa declarativa
-- de la base de datos (RNF-09): un grupo no tiene dos sesiones el mismo día (R8) y un docente o un aula
-- no empiezan dos sesiones a la misma hora en la misma versión (R1 y R2; los solapes parciales los
-- rechaza el validador del backend antes de guardar).
CREATE TABLE sesion (
    id          VARCHAR(40) NOT NULL,
    version_id  VARCHAR(40) NOT NULL,
    grupo_id    VARCHAR(40) NOT NULL,
    dia         INT         NOT NULL,
    inicio      INT         NOT NULL,
    docente_id  VARCHAR(40),
    aula_id     VARCHAR(40),
    CONSTRAINT pk_sesion PRIMARY KEY (id),
    CONSTRAINT uk_sesion_grupo_dia UNIQUE (version_id, grupo_id, dia),
    CONSTRAINT uk_sesion_docente UNIQUE (version_id, docente_id, dia, inicio),
    CONSTRAINT uk_sesion_aula UNIQUE (version_id, aula_id, dia, inicio),
    CONSTRAINT fk_sesion_version FOREIGN KEY (version_id) REFERENCES version_horario (id) ON DELETE CASCADE,
    CONSTRAINT fk_sesion_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id),
    CONSTRAINT fk_sesion_docente FOREIGN KEY (docente_id) REFERENCES docente (id),
    CONSTRAINT fk_sesion_aula FOREIGN KEY (aula_id) REFERENCES aula (id),
    CONSTRAINT ck_sesion_dia CHECK (dia BETWEEN 0 AND 5),
    CONSTRAINT ck_sesion_inicio CHECK (inicio BETWEEN 0 AND 23)
);

-- Grupos que el generador no pudo ubicar, con la regla y la causa
CREATE TABLE grupo_pendiente (
    version_id  VARCHAR(40)  NOT NULL,
    grupo_id    VARCHAR(40)  NOT NULL,
    regla       VARCHAR(5)   NOT NULL,
    causa       VARCHAR(600) NOT NULL,
    CONSTRAINT pk_grupo_pendiente PRIMARY KEY (version_id, grupo_id),
    CONSTRAINT fk_pend_version FOREIGN KEY (version_id) REFERENCES version_horario (id) ON DELETE CASCADE,
    CONSTRAINT fk_pend_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id)
);

-- ------------------------------------------------------------------ usuarios, comité y auditoría (RF-15, RF-20, RF-22)
CREATE TABLE usuario (
    id          VARCHAR(40)  NOT NULL,
    nombre      VARCHAR(120) NOT NULL,
    correo      VARCHAR(120) NOT NULL,
    hash_clave  VARCHAR(100) NOT NULL,
    rol         VARCHAR(20)  NOT NULL,
    activo      BOOLEAN      NOT NULL,
    docente_id  VARCHAR(40),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_docente FOREIGN KEY (docente_id) REFERENCES docente (id) ON DELETE SET NULL,
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('COORDINADOR', 'COMITE', 'DOCENTE', 'ADMINISTRADOR'))
);

CREATE TABLE observacion (
    id          VARCHAR(40)   NOT NULL,
    version_id  VARCHAR(40)   NOT NULL,
    usuario_id  VARCHAR(40)   NOT NULL,
    autor       VARCHAR(120)  NOT NULL,
    concepto    VARCHAR(15)   NOT NULL,
    texto       VARCHAR(2000) NOT NULL,
    fecha       DATETIME(3)   NOT NULL,
    CONSTRAINT pk_observacion PRIMARY KEY (id),
    CONSTRAINT fk_obs_version FOREIGN KEY (version_id) REFERENCES version_horario (id) ON DELETE CASCADE,
    CONSTRAINT fk_obs_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_obs_concepto CHECK (concepto IN ('FAVORABLE', 'CON_AJUSTES'))
);

-- Bitácora de auditoría: el backend solo inserta (no hay operaciones de modificación ni borrado)
CREATE TABLE bitacora (
    id       BIGINT        NOT NULL AUTO_INCREMENT,
    fecha    DATETIME(3)   NOT NULL,
    usuario  VARCHAR(120)  NOT NULL,
    rol      VARCHAR(20)   NOT NULL,
    accion   VARCHAR(80)   NOT NULL,
    detalle  VARCHAR(1000) NOT NULL,
    CONSTRAINT pk_bitacora PRIMARY KEY (id)
);
CREATE INDEX ix_bitacora_fecha ON bitacora (fecha);

-- ------------------------------------------------------------------ horario de atención por materia (RF-23)
CREATE TABLE horario_atencion (
    id          VARCHAR(40)  NOT NULL,
    periodo_id  VARCHAR(40)  NOT NULL,
    docente_id  VARCHAR(40)  NOT NULL,
    curso_id    VARCHAR(40)  NOT NULL,
    modo        VARCHAR(10)  NOT NULL,
    dia         INT,
    inicio      INT,
    fin         INT,
    lugar       VARCHAR(200) NOT NULL,
    CONSTRAINT pk_horario_atencion PRIMARY KEY (id),
    CONSTRAINT uk_atencion UNIQUE (periodo_id, docente_id, curso_id),
    CONSTRAINT fk_atencion_periodo FOREIGN KEY (periodo_id) REFERENCES periodo_academico (id) ON DELETE CASCADE,
    CONSTRAINT fk_atencion_docente FOREIGN KEY (docente_id) REFERENCES docente (id) ON DELETE CASCADE,
    CONSTRAINT fk_atencion_curso FOREIGN KEY (curso_id) REFERENCES curso (id) ON DELETE CASCADE,
    CONSTRAINT ck_atencion_modo CHECK (modo IN ('HORARIO', 'ACORDAR'))
);
