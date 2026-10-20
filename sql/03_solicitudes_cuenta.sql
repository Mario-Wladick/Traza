-- =========================================================
-- TRAZA · Paso 3: solicitudes de cuenta
-- Para bases que YA tienen 01 y 02 cargados (no borra datos).
-- Ejecutar conectado a la base «traza».
--
-- Quien no tiene cuenta la pide desde «Regístrate» en la página
-- de inicio; la secretaría académica la aprueba o la rechaza.
-- Al aprobarla se crea el usuario con la contraseña que eligió.
-- =========================================================

SET ROLE traza_app;   -- las tablas quedan a nombre de traza_app

CREATE TABLE IF NOT EXISTS solicitud_cuenta (
    id             SERIAL       PRIMARY KEY,
    codigo         VARCHAR(6)   NOT NULL CHECK (codigo ~ '^[0-9]{6}$'),
    nombre         VARCHAR(120) NOT NULL,
    rol            VARCHAR(12)  NOT NULL CHECK (rol IN ('estudiante', 'docente')),
    clave_cifrada  VARCHAR(200) NOT NULL,
    estado         VARCHAR(10)  NOT NULL DEFAULT 'pendiente'
                   CHECK (estado IN ('pendiente', 'aprobada', 'rechazada')),
    creada_en      TIMESTAMP    NOT NULL DEFAULT now(),
    atendida_en    TIMESTAMP,
    atendida_por   VARCHAR(6)   REFERENCES usuario (codigo)
);

-- Una sola solicitud pendiente por código
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_pendiente
    ON solicitud_cuenta (codigo) WHERE estado = 'pendiente';
