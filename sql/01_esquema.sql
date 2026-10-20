-- =========================================================
-- TRAZA · Sistema de Control Educativo con Portafolio Automático
-- Esquema de la base de datos (PostgreSQL 14 o superior)
--
-- Sale del modelo de contenido de la Práctica 3:
--   Usuario, Curso, Asistencia, Evidencia y Portafolio.
-- El Portafolio NO es tabla: «armarPortafolio no cambia datos»,
-- se arma con la vista v_portafolio_estudiante_curso.
--
-- Ejecutar conectado a la base «traza» (ver 00_crear_base.sql).
-- =========================================================

DROP VIEW  IF EXISTS v_portafolio_estudiante_curso;
DROP TABLE IF EXISTS evidencia;
DROP TABLE IF EXISTS asistencia;
DROP TABLE IF EXISTS matricula;
DROP TABLE IF EXISTS curso;
DROP TABLE IF EXISTS usuario;

-- ---------------------------------------------------------
-- 1 · USUARIO (RF-06, RF-02)
-- rol: estudiante | docente | secretaria | director
-- La contraseña se guarda cifrada con PBKDF2 (nunca en texto plano).
-- ---------------------------------------------------------
CREATE TABLE usuario (
    codigo         VARCHAR(6)   PRIMARY KEY
                   CHECK (codigo ~ '^[0-9]{6}$'),
    nombre         VARCHAR(120) NOT NULL,
    rol            VARCHAR(12)  NOT NULL
                   CHECK (rol IN ('estudiante', 'docente', 'secretaria', 'director')),
    clave_cifrada  VARCHAR(200) NOT NULL,
    creado_en      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------
-- 2 · CURSO (RF-01, RF-02, RF-05)
-- Código único por semestre (INF301 en 2026-II).
-- estado_portafolio: abierto | cerrado | revisado
-- ---------------------------------------------------------
CREATE TABLE curso (
    id                 SERIAL       PRIMARY KEY,
    codigo             VARCHAR(10)  NOT NULL,
    nombre             VARCHAR(120) NOT NULL,
    semestre           VARCHAR(7)   NOT NULL CHECK (semestre ~ '^[0-9]{4}-(I|II)$'),
    ciclo              VARCHAR(4)   NOT NULL
                       CHECK (ciclo IN ('I','II','III','IV','V','VI','VII','VIII','IX','X')),
    docente_codigo     VARCHAR(6)   NOT NULL REFERENCES usuario (codigo),
    estado_portafolio  VARCHAR(8)   NOT NULL DEFAULT 'abierto'
                       CHECK (estado_portafolio IN ('abierto', 'cerrado', 'revisado')),
    CONSTRAINT uq_curso_semestre UNIQUE (codigo, semestre)
);

-- ---------------------------------------------------------
-- MATRÍCULA: vincula al estudiante con los cursos del semestre (RF-02)
-- La clave única impide matricular dos veces en el mismo curso:
-- «Ya está matriculado en este curso».
-- ---------------------------------------------------------
CREATE TABLE matricula (
    id                 SERIAL     PRIMARY KEY,
    estudiante_codigo  VARCHAR(6) NOT NULL REFERENCES usuario (codigo),
    curso_id           INTEGER    NOT NULL REFERENCES curso (id) ON DELETE CASCADE,
    matriculado_en     TIMESTAMP  NOT NULL DEFAULT now(),
    CONSTRAINT uq_matricula UNIQUE (estudiante_codigo, curso_id)
);

-- ---------------------------------------------------------
-- 3 · ASISTENCIA (RF-01)
-- Un registro por estudiante, por sesión, por curso.
-- Solo de estudiantes matriculados (clave foránea a matrícula).
-- ---------------------------------------------------------
CREATE TABLE asistencia (
    id                 SERIAL     PRIMARY KEY,
    curso_id           INTEGER    NOT NULL,
    sesion             SMALLINT   NOT NULL CHECK (sesion > 0),
    estudiante_codigo  VARCHAR(6) NOT NULL,
    fecha_sesion       DATE       NOT NULL,
    estado             VARCHAR(8) NOT NULL CHECK (estado IN ('presente', 'tardanza', 'falta')),
    hora_registro      TIME       NOT NULL DEFAULT localtime(0),
    CONSTRAINT uq_asistencia UNIQUE (curso_id, sesion, estudiante_codigo),
    CONSTRAINT fk_asistencia_matricula FOREIGN KEY (estudiante_codigo, curso_id)
        REFERENCES matricula (estudiante_codigo, curso_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------
-- 4 · EVIDENCIA (RF-03, RF-04)
-- PDF o imagen, hasta 10 MB. Una sola por tipo y unidad (flujo 3b).
-- calificacion: entero de 0 a 20, vacía (NULL) hasta que el docente califica.
-- ---------------------------------------------------------
CREATE TABLE evidencia (
    id                 SERIAL       PRIMARY KEY,
    estudiante_codigo  VARCHAR(6)   NOT NULL,
    curso_id           INTEGER      NOT NULL,
    unidad             VARCHAR(3)   NOT NULL CHECK (unidad IN ('I', 'II', 'III')),
    tipo               VARCHAR(10)  NOT NULL CHECK (tipo IN ('trabajo', 'práctica', 'proyecto', 'examen')),
    archivo_nombre     VARCHAR(200) NOT NULL,
    archivo_ruta       VARCHAR(300),                     -- dónde quedó guardado en el servidor
    archivo_formato    VARCHAR(6)   NOT NULL CHECK (archivo_formato IN ('PDF', 'Imagen')),
    tamano_mb          NUMERIC(4,1) NOT NULL CHECK (tamano_mb >= 0 AND tamano_mb <= 10),
    fecha_entrega      DATE         NOT NULL DEFAULT current_date,
    calificacion       SMALLINT     CHECK (calificacion BETWEEN 0 AND 20),
    calificado_en      TIMESTAMP,
    CONSTRAINT uq_evidencia UNIQUE (estudiante_codigo, curso_id, unidad, tipo),
    CONSTRAINT fk_evidencia_matricula FOREIGN KEY (estudiante_codigo, curso_id)
        REFERENCES matricula (estudiante_codigo, curso_id) ON DELETE CASCADE
);

CREATE INDEX ix_curso_docente       ON curso (docente_codigo);
CREATE INDEX ix_matricula_curso     ON matricula (curso_id);
CREATE INDEX ix_asistencia_est      ON asistencia (estudiante_codigo, curso_id);
CREATE INDEX ix_evidencia_curso     ON evidencia (curso_id, calificacion);

-- ---------------------------------------------------------
-- 5 · PORTAFOLIO (RF-05): se arma solo, sin que nadie suba nada aparte.
-- Por cada estudiante y curso: asistencia, entregas, promedio y avance.
-- Avance = evidencias distintas entregadas / 6 esperadas
--          (3 unidades × trabajo y práctica).
-- ---------------------------------------------------------
CREATE VIEW v_portafolio_estudiante_curso AS
SELECT m.estudiante_codigo,
       m.curso_id,
       COUNT(DISTINCT a.id) FILTER (WHERE a.estado = 'presente') AS presentes,
       COUNT(DISTINCT a.id) FILTER (WHERE a.estado = 'tardanza') AS tardanzas,
       COUNT(DISTINCT a.id) FILTER (WHERE a.estado = 'falta')    AS faltas,
       COUNT(DISTINCT a.id)                                      AS sesiones,
       COUNT(DISTINCT e.id)                                      AS entregadas,
       COUNT(DISTINCT e.id) FILTER (WHERE e.calificacion IS NULL) AS por_calificar,
       ROUND(AVG(e.calificacion), 1)                             AS promedio,
       LEAST(100, ROUND(COUNT(DISTINCT (e.unidad, e.tipo)) * 100.0 / 6))::INT AS avance
FROM matricula m
LEFT JOIN asistencia a ON a.estudiante_codigo = m.estudiante_codigo AND a.curso_id = m.curso_id
LEFT JOIN evidencia  e ON e.estudiante_codigo = m.estudiante_codigo AND e.curso_id = m.curso_id
GROUP BY m.estudiante_codigo, m.curso_id;
