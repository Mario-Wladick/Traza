-- =========================================================
-- TRAZA · Paso 0: crear la base de datos y su usuario
-- Ejecutar UNA vez conectado como «postgres» (pgAdmin o psql).
-- =========================================================

CREATE USER traza_app WITH PASSWORD 'traza2026';

CREATE DATABASE traza
    WITH OWNER = traza_app
         ENCODING = 'UTF8'
         TEMPLATE = template0;

-- Después: conéctate a la base «traza» como traza_app y ejecuta
--   01_esquema.sql
--   02_datos_prueba.sql
